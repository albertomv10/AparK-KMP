package com.albertomedina.apark.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun ShareTextHandler(
    trigger: Int,
    text: String,
    onShared: () -> Unit
) {
    LaunchedEffect(trigger) {
        if (trigger > 0 && text.isNotBlank()) {
            val controller = UIActivityViewController(
                activityItems = listOf(text),
                applicationActivities = null
            )
            UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
                controller,
                animated = true,
                completion = null
            )
            onShared()
        }
    }
}
