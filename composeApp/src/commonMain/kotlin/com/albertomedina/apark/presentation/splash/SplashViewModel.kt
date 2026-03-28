package com.albertomedina.apark.presentation.splash

import androidx.lifecycle.ViewModel
import com.albertomedina.apark.domain.repository.AuthRepository


class SplashViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val isUserLoggedIn: Boolean
        get() = authRepository.isUserLoggedIn()

    val isUserEmailVerified: Boolean
        get() = authRepository.isUserEmailVerified()
}
