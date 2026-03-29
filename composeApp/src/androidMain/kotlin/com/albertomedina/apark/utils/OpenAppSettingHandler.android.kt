package com.albertomedina.apark.utils

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun OpenAppSettingsHandler(
    trigger: Int,
    onSettingsOpened: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
            onSettingsOpened()
        }
    }
}