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
            println("Firestore: Error al crear usuario -> ${e.message}")
        }
    }
}
