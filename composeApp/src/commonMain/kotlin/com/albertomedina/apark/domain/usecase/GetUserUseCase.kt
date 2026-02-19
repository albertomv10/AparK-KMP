package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.User
import com.albertomedina.apark.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class GetUserUseCase(
    private val repository: UserRepository
) {
    operator fun invoke(userId: String): Flow<User> {
        return repository.getUser(userId)
    }
}