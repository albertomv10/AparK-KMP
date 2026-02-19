package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        return authRepository.login(email, password)
    }
}