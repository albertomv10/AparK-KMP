package com.albertomedina.apark.data.repository

import com.albertomedina.apark.data.util.FirestoreConstants
import com.albertomedina.apark.domain.model.User
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.Vehicle.LocationModel
import com.albertomedina.apark.domain.repository.VehicleRepository
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class FirestoreVehicleRepository(
    private val firestore: FirebaseFirestore
) : VehicleRepository {

    /**
     * Los vehículos salen de **una consulta**, no de recorrer una lista de ids.
     *
     * Firestore acepta esta consulta porque puede demostrar, solo con su filtro, que nunca devolverá
     * un documento que la regla prohíba. De ahí que aquí ya no haga falta el `retryWhen` que hubo
     * durante un tiempo: escuchar un vehículo inexistente devolvía `PERMISSION_DENIED` —la regla
     * desreferenciaba un `resource` nulo— y sin reintentos la app llegó a crashear. Ese caso ya no
     * puede darse: lo que no está en el resultado, sencillamente no se pide.
     *
     * Son **dos** listeners y no N+1: la consulta, y el documento propio del usuario, que solo
     * aporta el orden.
     */
    override fun getVehiclesForUser(userId: String): Flow<List<Vehicle>> {
        val vehicles = firestore.collection(FirestoreConstants.CARS_COLLECTION)
            .where { FirestoreConstants.MEMBER_IDS_FIELD contains userId }
            .snapshots
            .map { snapshot ->
                // El id autoritativo es el del documento, no el campo `id` que se copia dentro.
                snapshot.documents.map { it.data<Vehicle>().copy(id = it.id) }
            }

        val order = firestore.collection(FirestoreConstants.USERS_COLLECTION)
            .document(userId)
            .snapshots
            .map { if (it.exists) it.data<User>().userVehicles else emptyList() }

        return combine(vehicles, order, ::sortByHint)
    }

    /**
     * Ordena por la pista del usuario. Los vehículos que no aparezcan en ella van al final
     * conservando el orden de la consulta, porque `sortedBy` es estable — así, un vehículo recién
     * compartido aparece aunque su id todavía no esté en la lista de nadie.
     */
    private fun sortByHint(vehicles: List<Vehicle>, hint: List<String>): List<Vehicle> {
        val position = hint.withIndex().associate { (index, id) -> id to index }
        return vehicles.sortedBy { position[it.id] ?: Int.MAX_VALUE }
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

            if (!vehicle.memberIds.contains(userId) && !user.userVehicles.contains(vehicleId)) {
                update(carRef, FirestoreConstants.MEMBER_IDS_FIELD to FieldValue.arrayUnion(userId))
                update(userRef, FirestoreConstants.CARS_FIELD to FieldValue.arrayUnion(vehicleId))
            }
        }
    }

    override suspend fun createVehicle(userId: String, name: String, licensePlate: String): Result<Unit> {
        return try {
            val carRef = firestore.collection(FirestoreConstants.CARS_COLLECTION).document

            val now = Clock.System.now().toEpochMilliseconds()
            val newVehicle = Vehicle(
                id = carRef.id,
                name = name,
                licensePlate = licensePlate,
                ownerId = userId,
                // El dueño va dentro desde el principio: la regla `create` exige exactamente esto,
                // porque un vehículo cuyo dueño no es miembro sería ilegible para él mismo.
                memberIds = listOf(userId),
                lastLocation = null,
                createdAt = now,
                updatedAt = now
            )

            val batch = firestore.batch()
            batch.set(carRef, newVehicle)
            val userRef = firestore.collection(FirestoreConstants.USERS_COLLECTION).document(userId)
            batch.update(userRef, FirestoreConstants.CARS_FIELD to FieldValue.arrayUnion(carRef.id))
            batch.commit()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteVehicle(vehicleId: String, userId: String): Result<Unit> {
        return try {
            val carRef = firestore.collection(FirestoreConstants.CARS_COLLECTION).document(vehicleId)
            val userRef = firestore.collection(FirestoreConstants.USERS_COLLECTION).document(userId)

            // Deletes the vehicle and drops it from the caller's own list atomically. Other
            // members keep a dangling id until the cleanup function (spec 002) exists; the
            // vehicle stream already discards those.
            val batch = firestore.batch()
            batch.delete(carRef)
            batch.update(userRef, FirestoreConstants.CARS_FIELD to FieldValue.arrayRemove(vehicleId))
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
                    "color" to vehicle.color,
                    FirestoreConstants.UPDATED_AT_FIELD to Clock.System.now().toEpochMilliseconds()
                )
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
            batch.update(carRef, FirestoreConstants.MEMBER_IDS_FIELD to FieldValue.arrayRemove(userId))
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

}
