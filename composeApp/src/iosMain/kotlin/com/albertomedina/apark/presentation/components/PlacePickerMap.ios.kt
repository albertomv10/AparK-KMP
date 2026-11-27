package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.albertomedina.apark.ui.theme.LocalIsDarkTheme
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

/**
 * La fábrica del mapa de elegir el punto (spec 024): latitud, longitud, zoom y el JSON del estilo
 * oscuro, o `null` para el normal. La pone Swift (`iOSApp.swift`), con su propio controlador: el mapa
 * del carrusel es único y vive en `MapLocationController.shared`, y no debe enterarse de este.
 */
var iosPlacePickerFactory: ((Double, Double, Double, String?) -> UIView)? = null

/** Swift avisa aquí del centro del mapa cada vez que la cámara se para. */
var iosOnPickerCenterChanged: ((Double, Double) -> Unit)? = null

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlacePickerMap(
    modifier: Modifier,
    initial: GeoPoint,
    initialZoom: Float,
    onCenterChanged: (GeoPoint) -> Unit
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val currentOnCenterChanged by rememberUpdatedState(onCenterChanged)

    DisposableEffect(Unit) {
        iosOnPickerCenterChanged = { lat, lng -> currentOnCenterChanged(GeoPoint(lat, lng)) }
        onDispose { iosOnPickerCenterChanged = null }
    }

    UIKitView(
        factory = {
            iosPlacePickerFactory?.invoke(
                initial.latitude,
                initial.longitude,
                initialZoom.toDouble(),
                if (isDarkTheme) darkMapStyleJson else null
            ) ?: UIView()
        },
        modifier = modifier
    )
}
