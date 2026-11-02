package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.auth.AuthFailure
import com.albertomedina.apark.domain.auth.AuthFailureReason
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.repository.UserRepository

/** El mismo tope que `firestore.rules` y que la función `propagateUserProfile`. */
const val USER_NAME_MAX_LENGTH = 40

enum class UserNameError { EMPTY, TOO_LONG }

/** Qué tiene de malo un nombre, ya recortado, o `null` si vale. */
fun validateUserName(name: String): UserNameError? = when {
    name.isBlank() -> UserNameError.EMPTY
    name.trim().length > USER_NAME_MAX_LENGTH -> UserNameError.TOO_LONG
    else -> null
}

class InvalidUserNameException(val error: UserNameError) : Exception(error.name)

/**
 * Cambia el nombre que ven los demás miembros. Solo escribe `users/{uid}.name`: las copias que
 * llevan los vehículos las pone al día la función `propagateUserProfile`, así que en las tarjetas
 * tarda un par de segundos.
 */
class UpdateUserNameUseCase(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(name: String): Result<Unit> {
        validateUserName(name)?.let { return Result.failure(InvalidUserNameException(it)) }
        val uid = authRepository.getCurrentUser()?.uid
            ?: return Result.failure(AuthFailure(AuthFailureReason.SESSION_EXPIRED))
        return userRepository.updateName(uid, name.trim())
    }
}
