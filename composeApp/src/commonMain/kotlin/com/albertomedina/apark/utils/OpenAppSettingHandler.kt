package com.albertomedina.apark.utils

import androidx.compose.runtime.Composable

// Un componente invisible que abrirá los ajustes cuando se lo pidamos
@Composable
expect fun OpenAppSettingsHandler(
    trigger: Int,
    onSettingsOpened: () -> Unit
)