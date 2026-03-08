package com.albertomedina.apark.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.albertomedina.apark.ui.theme.onSuccess
import com.albertomedina.apark.ui.theme.success

sealed class SnackbarMessage(
    open val message: String,
    open val actionLabel: String? = null,
    open val onAction: (() -> Unit)? = null
) {
    @Composable
    open fun backgroundColor(): Color = MaterialTheme.colorScheme.surface

    @Composable
    open fun contentColor(): Color = MaterialTheme.colorScheme.onSurface

    // ---- Tipos ----
    data class Success(
        override val message: String,
        override val actionLabel: String? = null,
        override val onAction: (() -> Unit)? = null
    ) : SnackbarMessage(message, actionLabel, onAction) {
        @Composable
        override fun backgroundColor(): Color = MaterialTheme.colorScheme.success

        @Composable
        override fun contentColor(): Color = MaterialTheme.colorScheme.onSuccess
    }

    data class Error(
        override val message: String,
        override val actionLabel: String? = null,
        override val onAction: (() -> Unit)? = null
    ) : SnackbarMessage(message, actionLabel, onAction) {
        @Composable
        override fun backgroundColor(): Color = MaterialTheme.colorScheme.error

        @Composable
        override fun contentColor(): Color = MaterialTheme.colorScheme.onError
    }

    data class Info(
        override val message: String,
        override val actionLabel: String? = null,
        override val onAction: (() -> Unit)? = null
    ) : SnackbarMessage(message, actionLabel, onAction) {
        @Composable
        override fun backgroundColor(): Color = MaterialTheme.colorScheme.surfaceVariant

        @Composable
        override fun contentColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant
    }
}
