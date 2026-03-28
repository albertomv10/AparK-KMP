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
import kotlinx.coroutines.delay
import platform.UIKit.UIView

data class IosMapMarker(val id: String, val lat: Double, val lng: Double, val title: String)
var iosMapViewFactory: (() -> UIView)? = null
var updateMapPadding: ((Double) -> Unit)? = null

var iosMapUpdateCamera: ((Double, Double, Boolean) -> Unit)? = null
var iosMapUpdateMarkers: ((List<IosMapMarker>) -> Unit)? = null
var iosCenterOnUserLocation: ((Boolean) -> Unit)? = null

var iosOnMarkerDragged: ((String, Double, Double) -> Unit)? = null
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AparKMap(
    modifier: Modifier,
    bottomPadding: Dp,
    vehicles: List<Vehicle>,
    selectedVehicleIndex: Int,
    centerCameraTrigger:Int,
    onMarkerDragged: (String, Double, Double) -> Unit
) {
    var isFirstLoad by remember { mutableStateOf(true) }
    //Avisar a Swift de los marcadores
    LaunchedEffect(vehicles) {
        val markers = vehicles.mapNotNull { vehicle ->
            vehicle.lastLocation?.let { loc ->
                IosMapMarker(vehicle.id, loc.latitude, loc.longitude, vehicle.name)
            }
        }
        iosMapUpdateMarkers?.invoke(markers)
    }

    //Avisar a Swift para mover la cámara
    LaunchedEffect(selectedVehicleIndex, vehicles) {
        if (vehicles.isNotEmpty() && selectedVehicleIndex < vehicles.size) {
            val loc = vehicles[selectedVehicleIndex].lastLocation
            if (loc != null) {
                iosMapUpdateCamera?.invoke(loc.latitude, loc.longitude, !isFirstLoad)
                isFirstLoad = false
            }
        }
    }

    LaunchedEffect(vehicles.size) {
        if (vehicles.isEmpty()) {
            delay(200)
            iosCenterOnUserLocation?.invoke(!isFirstLoad)
            isFirstLoad = false
        }
    }

    LaunchedEffect(centerCameraTrigger) {
        if (centerCameraTrigger > 0) {
            iosCenterOnUserLocation?.invoke(true)
        }
    }

    LaunchedEffect(onMarkerDragged) {
        iosOnMarkerDragged = { id, lat, lng ->
            onMarkerDragged(id, lat, lng)
        }
    }

    UIKitView(
        factory = {
            // Llamamos a la función que Swift nos proporcionó
            iosMapViewFactory?.invoke() ?: UIView()
        },
        update = {
            updateMapPadding?.invoke(bottomPadding.value.toDouble()-30.0)
        },
        modifier = modifier
    )
}