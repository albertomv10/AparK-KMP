package com.albertomedina.apark.utils

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun ShareTextHandler(
    trigger: Int,
    text: String,
    onShared: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(trigger) {
        if (trigger > 0 && text.isNotBlank()) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(sendIntent, null))
            onShared()
        }
    }
}
