package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.auth.AuthFailure
import com.albertomedina.apark.domain.auth.AuthFailureReason
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.repository.UserRepository

/**
 * Pone `users/{uid}.email` al día con el de la cuenta.
 *
 * Hace falta porque el email cambia **fuera de la app**: al abrir el enlace de verificación. Firebase
 * cierra entonces la sesión, así que la siguiente vez que se entra ya es con el nuevo, y este es el
 * momento de alcanzarlo. Escribirlo dispara `propagateUserProfile`, que lo copia a las tarjetas.
 */
class SyncUserProfileUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val user = authRepository.getCurrentUser()
            ?: return Result.failure(AuthFailure(AuthFailureReason.SESSION_EXPIRED))
        val email = user.email ?: return Result.success(Unit)
        return userRepository.syncEmail(user.uid, email)
    }
}
