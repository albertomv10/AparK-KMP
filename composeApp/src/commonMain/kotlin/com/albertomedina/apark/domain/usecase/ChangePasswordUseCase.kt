package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.AuthRepository

/** Cambia la contraseña, reautenticando antes con la actual por la misma razón que el email. */
class ChangePasswordUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(currentPassword: String, newPassword: String): Result<Unit> =
        authRepository.reauthenticate(currentPassword)
            .fold(onSuccess = { authRepository.changePassword(newPassword) }, onFailure = { Result.failure(it) })
}
