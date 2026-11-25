package com.albertomedina.apark.data.repository

import com.albertomedina.apark.data.util.FirestoreConstants
import com.albertomedina.apark.domain.model.User
import com.albertomedina.apark.domain.repository.UserRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

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

    override suspend fun setVehicleOrder(userId: String, vehicleIds: List<String>): Result<Unit> = try {
        // Una escritura normal, no la transacción de antes (spec 016): la transacción necesita el
        // servidor y sin conexión fallaba. Esta se encola, y el caché local la enseña al momento.
        firestore.collection(FirestoreConstants.USERS_COLLECTION).document(userId).update(
            FirestoreConstants.CARS_FIELD to vehicleIds,
            FirestoreConstants.UPDATED_AT_FIELD to Clock.System.now().toEpochMilliseconds()
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateName(userId: String, name: String): Result<Unit> = try {
        firestore.collection(FirestoreConstants.USERS_COLLECTION).document(userId).update(
            FirestoreConstants.NAME_FIELD to name,
            FirestoreConstants.UPDATED_AT_FIELD to Clock.System.now().toEpochMilliseconds()
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun syncEmail(userId: String, email: String): Result<Unit> = try {
        val docRef = firestore.collection(FirestoreConstants.USERS_COLLECTION).document(userId)
        val snapshot = docRef.get()
        // Sin documento todavía no hay nada que alinear: lo crea la verificación o el primer login
        // social, ya con el email bueno. Y si coincide, escribir dispararía la función para nada.
        if (snapshot.exists && snapshot.data<User>().email != email) {
            docRef.update(
                FirestoreConstants.EMAIL_FIELD to email,
                FirestoreConstants.UPDATED_AT_FIELD to Clock.System.now().toEpochMilliseconds()
            )
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun createUser(user: User) {
        try {
            val docRef = firestore.collection(FirestoreConstants.USERS_COLLECTION).document(user.id)
            val snapshot = docRef.get()

            if (!snapshot.exists) {
                // Las fechas las pone quien crea el documento, y este es el único sitio que lo
                // hace: quien llama pasa un `User` recién construido, con las dos a 0, y así
                // llegaban a Firestore.
                val now = Clock.System.now().toEpochMilliseconds()
                docRef.set<User>(user.copy(createdAt = now, updatedAt = now))
            }
        } catch (e: Exception) {
            println("Firestore: Error al crear usuario -> ${e.message}")
        }
    }
}
