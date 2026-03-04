package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseUser // 👈 Ojo al import KMP

class LoginGoogleUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String, accessToken: String? = null): Result<FirebaseUser?> {
        return authRepository.loginWithGoogle(idToken, accessToken)
    }
}