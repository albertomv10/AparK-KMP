package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun GoogleSignInButton(
    modifier: Modifier = Modifier,
    buttonText: String,
    onTokenReceived: (String, String?) -> Unit,
    onError: (String) -> Unit
)