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
}