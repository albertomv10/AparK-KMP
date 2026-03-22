package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.albertomedina.apark.domain.model.Vehicle
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

data class IosMapMarker(val id: String, val lat: Double, val lng: Double, val title: String)
var iosMapViewFactory: (() -> UIView)? = null
var updateMapPadding: ((Double) -> Unit)? = null

var iosMapUpdateCamera: ((Double, Double, Boolean) -> Unit)? = null
var iosMapUpdateMarkers: ((List<IosMapMarker>) -> Unit)? = null
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AparKMap(
    modifier: Modifier,
    bottomPadding: Dp,
    vehicles: List<Vehicle>,
    selectedVehicleIndex: Int
) {
    var isFirstLoad by remember { mutableStateOf(true) }
    // 1. Avisar a Swift de los marcadores
    LaunchedEffect(vehicles) {
        val markers = vehicles.mapNotNull { vehicle ->
            vehicle.lastLocation?.let { loc ->
                IosMapMarker(vehicle.id, loc.latitude, loc.longitude, vehicle.name)
            }
        }
        iosMapUpdateMarkers?.invoke(markers)
    }

    // 2. Avisar a Swift para mover la cámara
    LaunchedEffect(selectedVehicleIndex, vehicles) {
        if (vehicles.isNotEmpty() && selectedVehicleIndex < vehicles.size) {
            val loc = vehicles[selectedVehicleIndex].lastLocation
            if (loc != null) {
                iosMapUpdateCamera?.invoke(loc.latitude, loc.longitude, !isFirstLoad)
                isFirstLoad = false
            }
        }
    }

    UIKitView(
        factory = {
            // Llamamos a la función que Swift nos proporcionó
            iosMapViewFactory?.invoke() ?: UIView()
        },
        update = {
            updateMapPadding?.invoke(bottomPadding.value.toDouble())
        },
        modifier = modifier
    )
}