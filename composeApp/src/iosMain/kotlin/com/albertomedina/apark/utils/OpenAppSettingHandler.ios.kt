package com.albertomedina.apark.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

@Composable
actual fun OpenAppSettingsHandler(
    trigger: Int,
    onSettingsOpened: () -> Unit
) {
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            val url = NSURL(string = UIApplicationOpenSettingsURLString)
            if (UIApplication.sharedApplication.canOpenURL(url)) {
                UIApplication.sharedApplication.openURL(
                    url = url,
                    options = emptyMap<Any?, Any>(),
                    completionHandler = null
                )
            }
            onSettingsOpened()
        }
    }
}