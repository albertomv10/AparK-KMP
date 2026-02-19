package com.albertomedina.apark.domain.repository

import com.albertomedina.apark.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(userId: String): Flow<User>
    suspend fun updateUserCars(userId: String, carIds: List<String>)
    suspend fun createUser(user: User)
}