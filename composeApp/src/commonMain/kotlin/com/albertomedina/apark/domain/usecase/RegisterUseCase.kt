package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.AuthRepository

class RegisterUseCase(
    private val authRepository: AuthRepository
) {
    // Regex compatible con KMP para validar emails
    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        if (!email.matches(emailRegex)) {
            return Result.failure(Exception("Formato de email inválido")) // Mensaje genérico o usar Resources
        }
        return authRepository.register(email, password)
    }
}