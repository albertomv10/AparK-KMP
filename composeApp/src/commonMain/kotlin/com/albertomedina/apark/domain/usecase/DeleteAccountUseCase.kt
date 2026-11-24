package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.AccountRepository
import com.albertomedina.apark.domain.repository.AuthRepository

/** Cómo demuestra la persona que es ella antes de borrar la cuenta. */
sealed interface DeletionProof {
    data class Password(val currentPassword: String) : DeletionProof
    data class Google(val idToken: String, val accessToken: String?) : DeletionProof
}

/**
 * Borra la cuenta (spec 012): reautentica y, si sale bien, pide al servidor que lo borre todo.
 *
 * No cierra la sesión: la pantalla enseña primero que la cuenta se ha eliminado, y cierra al
 * confirmarlo. Si la app se cierra antes, la sesión ya no vale en el servidor, y la siguiente
 * recarga del usuario la cierra sola.
 */
class DeleteAccountUseCase(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(proof: DeletionProof): Result<Unit> {
        val reauthenticated = when (proof) {
            is DeletionProof.Password -> authRepository.reauthenticate(proof.currentPassword)
            is DeletionProof.Google -> authRepository.reauthenticateWithGoogle(proof.idToken, proof.accessToken)
        }
        return reauthenticated.fold(onSuccess = { accountRepository.deleteAccount() }, onFailure = { Result.failure(it) })
    }
}
