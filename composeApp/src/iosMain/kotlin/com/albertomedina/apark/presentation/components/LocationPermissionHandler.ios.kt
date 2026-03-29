package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIApplicationDidBecomeActiveNotification

@Composable
actual fun LocationPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    fun checkIosPermission() {
        val status = CLLocationManager.authorizationStatus()
        val isGranted = status == kCLAuthorizationStatusAuthorizedWhenInUse ||
                status == kCLAuthorizationStatusAuthorizedAlways

        if (isGranted) onPermissionGranted() else onPermissionDenied()
    }

    DisposableEffect(Unit) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = null
        ) { _ ->
            checkIosPermission()
        }

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        checkIosPermission()
    }
}