package com.albertomedina.apark.utils

import androidx.compose.runtime.Composable

/**
 * Invisible component that opens the system share sheet with [text] when [trigger] changes,
 * following the same trigger-counter pattern as [OpenAppSettingsHandler].
 */
@Composable
expect fun ShareTextHandler(
    trigger: Int,
    text: String,
    onShared: () -> Unit
)
