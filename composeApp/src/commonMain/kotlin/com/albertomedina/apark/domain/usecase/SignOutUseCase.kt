package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.AuthRepository

class SignOutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit>{
        return  try {
            authRepository.logout()
            Result.success(Unit)
        }catch (e: Exception){
            Result.failure(e)
        }
    }
}