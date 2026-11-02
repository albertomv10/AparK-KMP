package com.albertomedina.apark.domain.repository

import com.albertomedina.apark.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(userId: String): Flow<User>
    suspend fun createUser(user: User)

    /**
     * Moves [vehicleId] [offset] positions within the user's own vehicle list, which is what
     * drives the order shown in the carousel. A move that would fall outside the list is a
     * no-op, not a failure.
     */
    suspend fun moveUserVehicle(userId: String, vehicleId: String, offset: Int): Result<Unit>

    /**
     * The name other members see on the cards of the vehicles this person parked. Only this field
     * is written: the copies inside the vehicles are the `propagateUserProfile` function's job.
     */
    suspend fun updateName(userId: String, name: String): Result<Unit>

    /**
     * Brings the stored email in line with the account's after it changed in Auth. Does nothing if
     * it already matches, or if the document does not exist yet.
     */
    suspend fun syncEmail(userId: String, email: String): Result<Unit>
}