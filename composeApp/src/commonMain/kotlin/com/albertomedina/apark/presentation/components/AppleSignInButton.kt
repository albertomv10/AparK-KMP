package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AppleSignInButton(
    modifier: Modifier = Modifier,
    onTokenReceived: (String, String) -> Unit, // (idToken, nonce)
    onError: (SocialLoginFailure) -> Unit
)