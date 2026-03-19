package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

// Variable global que Swift inicializará antes de arrancar Compose
var iosMapViewFactory: (() -> UIView)? = null

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AparKMap(modifier: Modifier) {
    UIKitView(
        factory = {
            // Llamamos a la función que Swift nos proporcionó
            iosMapViewFactory?.invoke() ?: UIView()
        },
        modifier = modifier
    )
}