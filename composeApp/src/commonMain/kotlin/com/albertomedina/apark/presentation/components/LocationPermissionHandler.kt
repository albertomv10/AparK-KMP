package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable

@Composable
expect fun LocationPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
)