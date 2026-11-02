package com.albertomedina.apark.account

import com.albertomedina.apark.domain.auth.AuthFailure
import com.albertomedina.apark.domain.auth.AuthFailureReason
import com.albertomedina.apark.domain.auth.SignInProvider
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.ChangeEmailUseCase
import com.albertomedina.apark.domain.usecase.ChangePasswordUseCase
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * El contrato que importa de verdad: **primero se reautentica, y si falla no se toca nada**. Con la
 * contraseña actual mal, ni se manda el enlace ni se cambia la contraseña.
 */
class ChangeCredentialsUseCaseTest {

    private class FakeAuthRepository(private val reauthResult: Result<Unit>) : AuthRepository {
        val calls = mutableListOf<String>()

        override suspend fun reauthenticate(currentPassword: String): Result<Unit> {
            calls += "reauthenticate:$currentPassword"
            return reauthResult
        }

        override suspend fun requestEmailChange(newEmail: String): Result<Unit> {
            calls += "requestEmailChange:$newEmail"
            return Result.success(Unit)
        }

        override suspend fun changePassword(newPassword: String): Result<Unit> {
            calls += "changePassword:$newPassword"
            return Result.success(Unit)
        }

        override val authStateChanges: Flow<FirebaseUser?> = emptyFlow()
        override suspend fun login(email: String, password: String) = error("no usado")
        override suspend fun register(email: String, password: String) = error("no usado")
        override fun isUserLoggedIn() = error("no usado")
        override fun isUserEmailVerified() = error("no usado")
        override suspend fun sendEmailVerification() = error("no usado")
        override fun getCurrentUser(): FirebaseUser? = error("no usado")
        override fun getUserEmail(): String? = error("no usado")
        override suspend fun logout() = error("no usado")
        override suspend fun loginWithGoogle(idToken: String, accessToken: String?) = error("no usado")
        override suspend fun loginWithApple(idToken: String, nonce: String) = error("no usado")
        override suspend fun resetPassword(email: String) = error("no usado")
        override fun signInProvider(): SignInProvider = error("no usado")
        override suspend fun reloadUser() = error("no usado")
        override suspend fun tokenEmail(): String? = error("no usado")
    }

    private val wrongPassword = Result.failure<Unit>(AuthFailure(AuthFailureReason.INVALID_CREDENTIALS))

    @Test
    fun `email - con la contraseña mal no se manda el enlace`() = runTest {
        val auth = FakeAuthRepository(wrongPassword)

        val result = ChangeEmailUseCase(auth)("nuevo@apark.test", "mal")

        assertEquals(AuthFailureReason.INVALID_CREDENTIALS, (result.exceptionOrNull() as AuthFailure).reason)
        assertEquals(listOf("reauthenticate:mal"), auth.calls)
    }

    @Test
    fun `email - reautentica antes y manda el enlace al email recortado`() = runTest {
        val auth = FakeAuthRepository(Result.success(Unit))

        val result = ChangeEmailUseCase(auth)("  nuevo@apark.test ", "buena")

        assertTrue(result.isSuccess)
        assertEquals(listOf("reauthenticate:buena", "requestEmailChange:nuevo@apark.test"), auth.calls)
    }

    @Test
    fun `contraseña - con la actual mal no se cambia`() = runTest {
        val auth = FakeAuthRepository(wrongPassword)

        val result = ChangePasswordUseCase(auth)("mal", "Nueva1234")

        assertTrue(result.isFailure)
        assertEquals(listOf("reauthenticate:mal"), auth.calls)
    }

    @Test
    fun `contraseña - reautentica antes de cambiarla`() = runTest {
        val auth = FakeAuthRepository(Result.success(Unit))

        val result = ChangePasswordUseCase(auth)("buena", "Nueva1234")

        assertTrue(result.isSuccess)
        assertEquals(listOf("reauthenticate:buena", "changePassword:Nueva1234"), auth.calls)
    }
}
