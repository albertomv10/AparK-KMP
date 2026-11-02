package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.AuthRepository

/**
 * Pide el cambio de email. Reautentica **siempre** antes: Firebase lo exige si el login no es
 * reciente, no hay forma de saberlo de antemano, y la ventana es de minutos.
 *
 * Que salga bien quiere decir que el enlace se ha enviado, **no** que el email haya cambiado: eso
 * ocurre cuando se abre, y entonces Firebase cierra la sesión.
 */
class ChangeEmailUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(newEmail: String, currentPassword: String): Result<Unit> =
        authRepository.reauthenticate(currentPassword)
            .fold(onSuccess = { authRepository.requestEmailChange(newEmail.trim()) }, onFailure = { Result.failure(it) })
}
