package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.AuthRepository
import dev.gitlive.firebase.auth.FirebaseUser

class LoginAppleUseCase(
    private val authRepository: AuthRepository

) {
    suspend operator fun invoke (idToken: String, nonce: String): Result<FirebaseUser?>{
        return authRepository.loginWithApple(idToken, nonce)
    }
}