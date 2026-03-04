package com.albertomedina.apark.data.repository

import com.albertomedina.apark.data.util.FirestoreConstants
import com.albertomedina.apark.domain.model.User
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.Vehicle.LocationModel
import com.albertomedina.apark.domain.repository.UserRepository
import com.albertomedina.apark.domain.repository.VehicleRepository
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FirestoreRepository(
    private val firestore: FirebaseFirestore
) : VehicleRepository, UserRepository {

    // ==========================================
    // IMPLEMENTACIÓN VEHICLE REPOSITORY
    // ==========================================

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getVehiclesForUser(userId: String): Flow<List<Vehicle>> {
        return firestore.collection(FirestoreConstants.USERS_COLLECTION)
            .document(userId)
            .snapshots
            .map { snapshot ->
                if (!snapshot.exists) return@map emptyList<String>()
                snapshot.data<User>().userVehicles
            }
            .flatMapLatest { vehicleIds ->
                if (vehicleIds.isEmpty()) return@flatMapLatest flowOf(emptyList())

                val vehicleFlows = vehicleIds.map { vehicleId ->
                    firestore.collection(FirestoreConstants.CARS_COLLECTION)
                        .document(vehicleId)
                        .snapshots
                        .map { vehicleSnap ->
                            if (vehicleSnap.exists) vehicleSnap.data<Vehicle>() else null
                        }
                }

                combine(vehicleFlows) { vehiclesArray ->
                    vehiclesArray.filterNotNull().toList()
                }
            }
    }

    override suspend fun getVehicleById(vehicleId: String): Vehicle? {
        return try {
            val snapshot = firestore.collection(FirestoreConstants.CARS_COLLECTION)
                .document(vehicleId)
                .get()
            if (snapshot.exists) snapshot.data<Vehicle>() else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getLastVehicleLocation(vehicleId: String): LocationModel? {
        return try {
            val snapshot = firestore.collection(FirestoreConstants.CARS_COLLECTION)
                .document(vehicleId)
                .get()

            if (snapshot.exists) snapshot.data<Vehicle>().lastLocation else null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateVehicleLocation(vehicleId: String, location: LocationModel): Result<Unit> {
        return try {
            firestore.collection(FirestoreConstants.CARS_COLLECTION)
                .document(vehicleId)
                .update(FirestoreConstants.LAST_LOCATION_FIELD to location)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun shareVehicleWithUser(vehicleId: String, userId: String) {
        val carRef = firestore.collection(FirestoreConstants.CARS_COLLECTION).document(vehicleId)
        val userRef = firestore.collection(FirestoreConstants.USERS_COLLECTION).document(userId)

        firestore.runTransaction {
            val carSnapshot = get(carRef)
            val userSnapshot = get(userRef)

            val vehicle = carSnapshot.data<Vehicle>()
            val user = userSnapshot.data<User>()

            if (!vehicle.sharedUsers.contains(userId) && !user.userVehicles.contains(vehicleId)) {
                update(carRef, FirestoreConstants.SHARED_USERS_FIELD to FieldValue.arrayUnion(userId))
                update(userRef, FirestoreConstants.CARS_FIELD to FieldValue.arrayUnion(vehicleId))
            }
        }
    }

    override suspend fun createVehicle(userId: String, name: String): Result<Unit> {
        return try {
            val carRef = firestore.collection(FirestoreConstants.CARS_COLLECTION).document
            val inviteCode = generateInviteCode()

            val newVehicle = Vehicle(
                id = carRef.id,
                name = name,
                ownerId = userId,
                sharedUsers = emptyList(),
                inviteCode = inviteCode,
                lastLocation = null
            )

            // ⚠️ CORRECCIÓN BATCH: Instanciar, operar, commit
            val batch = firestore.batch()

            // 1. Crear documento del vehículo
            batch.set(carRef, newVehicle)

            // 2. Añadir ID del vehículo al usuario
            val userRef = firestore.collection(FirestoreConstants.USERS_COLLECTION).document(userId)
            batch.update(userRef, FirestoreConstants.CARS_FIELD to FieldValue.arrayUnion(carRef.id))

            // 3. Commit manual
            batch.commit()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateVehicle(vehicle: Vehicle): Result<Unit> {
        return try {
            firestore.collection(FirestoreConstants.CARS_COLLECTION)
                .document(vehicle.id)
                .update(
                    "name" to vehicle.name,
                    "licensePlate" to vehicle.licensePlate,
                    "model" to vehicle.model,
                    "color" to vehicle.color
                )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinVehicleByCodeOrId(identifier: String, userId: String): Result<Unit> {
        return try {
            val carCollection = firestore.collection(FirestoreConstants.CARS_COLLECTION)

            val vehicleSnapshot = if (identifier.length < 15) {
                val querySnapshot = carCollection.where(FirestoreConstants.INVITE_CODE_FIELD, equalTo = identifier).get()
                querySnapshot.documents.firstOrNull()
            } else {
                val snap = carCollection.document(identifier).get()
                if (snap.exists) snap else null
            }

            if (vehicleSnapshot == null || !vehicleSnapshot.exists) {
                return Result.failure(Exception("Vehículo no encontrado"))
            }

            val vehicle = vehicleSnapshot.data<Vehicle>()

            if (!vehicle.sharedUsers.contains(userId)) {
                // ⚠️ CORRECCIÓN BATCH
                val batch = firestore.batch()

                batch.update(
                    vehicleSnapshot.reference,
                    FirestoreConstants.SHARED_USERS_FIELD to FieldValue.arrayUnion(userId)
                )

                batch.update(
                    firestore.collection(FirestoreConstants.USERS_COLLECTION).document(userId),
                    FirestoreConstants.CARS_FIELD to FieldValue.arrayUnion(vehicle.id)
                )

                batch.commit()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeUserFromVehicle(vehicleId: String, userId: String): Result<Unit> {
        return try {
            val carRef = firestore.collection(FirestoreConstants.CARS_COLLECTION).document(vehicleId)
            val userRef = firestore.collection(FirestoreConstants.USERS_COLLECTION).document(userId)


            val batch = firestore.batch()

            batch.update(carRef, FirestoreConstants.SHARED_USERS_FIELD to FieldValue.arrayRemove(userId))
            batch.update(userRef, FirestoreConstants.CARS_FIELD to FieldValue.arrayRemove(vehicleId))

            batch.commit()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun transferVehicleOwnership(vehicleId: String, newOwnerId: String): Result<Unit> {
        return try {
            firestore.collection(FirestoreConstants.CARS_COLLECTION)
                .document(vehicleId)
                .update("ownerId" to newOwnerId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateInviteCode(): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..6).map { chars.random() }.joinToString("")
    }

    // ==========================================
    // IMPLEMENTACIÓN USER REPOSITORY
    // ==========================================

    override fun getUser(userId: String): Flow<User> {
        return firestore.collection(FirestoreConstants.USERS_COLLECTION)
            .document(userId)
            .snapshots
            .map {
                if (it.exists) it.data<User>() else User(id = userId)
            }
    }

    override suspend fun updateUserCars(userId: String, carIds: List<String>) {
        firestore.collection(FirestoreConstants.USERS_COLLECTION)
            .document(userId)
            .update(FirestoreConstants.CARS_FIELD to carIds)
    }

    override suspend fun createUser(user: User) {

        try {
            val docRef = firestore.collection(FirestoreConstants.USERS_COLLECTION).document(user.id)
            val snapshot = docRef.get()

            if (!snapshot.exists) {
                docRef.set<User>(user)
            }
        } catch (e: Exception) {
            println("Firestore: Error al crear usuario -> ${e.message}")        }
    }
}