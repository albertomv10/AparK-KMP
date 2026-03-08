package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AppleSignInButton(
    modifier: Modifier,
    onTokenReceived: (String, String) -> Unit,
    onError: (String) -> Unit
) {
}