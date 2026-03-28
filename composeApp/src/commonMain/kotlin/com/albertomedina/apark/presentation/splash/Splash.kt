package com.albertomedina.apark.presentation.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SplashViewModel = koinViewModel ()
) {
    LaunchedEffect(Unit) {
        if (viewModel.isUserLoggedIn && viewModel.isUserEmailVerified) {
            onNavigateToHome()
        } else {
            onNavigateToLogin()
        }
    }
}
