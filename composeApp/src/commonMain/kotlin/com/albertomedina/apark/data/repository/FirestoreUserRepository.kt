package com.albertomedina.apark.data.repository

import com.albertomedina.apark.data.util.FirestoreConstants
import com.albertomedina.apark.domain.model.User
import com.albertomedina.apark.domain.repository.UserRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore
) : UserRepository {

    override fun getUser(userId: String): Flow<User> {
        return firestore.collection(FirestoreConstants.USERS_COLLECTION)
            .document(userId)
            .snapshots
            .map {
                if (it.exists) it.data<User>() else User(id = userId)
            }
    }

    override suspend fun moveUserVehicle(userId: String, vehicleId: String, offset: Int): Result<Unit> {
        return try {
            val userRef = firestore.collection(FirestoreConstants.USERS_COLLECTION).document(userId)

            // Reordering means rewriting the whole array, so it must read the freshest server
            // state: a transaction retries on conflict, and a vehicle added meanwhile survives.
            firestore.runTransaction {
                val snapshot = get(userRef)
                if (!snapshot.exists) return@runTransaction

                val vehicleIds = snapshot.data<User>().userVehicles
                // Locate by id, never by an index coming from the UI: a stale list would
                // otherwise move the wrong vehicle.
                val from = vehicleIds.indexOf(vehicleId)
                val to = from + offset

                if (from == -1 || to !in vehicleIds.indices) return@runTransaction

                val reordered = vehicleIds.toMutableList().apply {
                    add(to, removeAt(from))
                }
                update(userRef, FirestoreConstants.CARS_FIELD to reordered)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createUser(user: User) {
        try {
            val docRef = firestore.collection(FirestoreConstants.USERS_COLLECTION).document(user.id)
            val snapshot = docRef.get()

            if (!snapshot.exists) {
                docRef.set<User>(user)
            }
        } catch (e: Exception) {
            println("Firestore: Error al crear usuario -> ${e.message}")
        }
    }
}
