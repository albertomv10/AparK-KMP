package com.albertomedina.apark.presentation.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.albertomedina.apark.domain.model.Vehicle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
actual fun AparKMap(
    modifier: Modifier,
    bottomPadding: Dp,
    vehicles: List<Vehicle>,
    selectedVehicleIndex: Int
) {

//    val context = LocalContext.current
//    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState()
    var isFirstLoad by remember { mutableStateOf(true) }

    // LaunchedEffect se ejecuta una sola vez cuando el Composable se carga
//    LaunchedEffect(Unit) {
//        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
//            if (location != null) {
//                // Si encontramos la ubicación, movemos la cámara con un zoom de 15 (nivel de calles)
//                cameraPositionState.position = CameraPosition.fromLatLngZoom(
//                    LatLng(location.latitude, location.longitude),
//                    15f
//                )
//            }
//        }
//    }

    LaunchedEffect(selectedVehicleIndex, vehicles) {
        if (vehicles.isNotEmpty() && selectedVehicleIndex < vehicles.size) {
            val location = vehicles[selectedVehicleIndex].lastLocation
            if (location != null) {
                // Preparamos el movimiento a la ciudad del coche con zoom 15
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    15f
                )

                if (isFirstLoad) {
                    // TELETRANSPORTE INSTANTÁNEO
                    cameraPositionState.move(cameraUpdate)
                    isFirstLoad = false
                } else {
                    // ANIMACIÓN SUAVE AL DESLIZAR
                    cameraPositionState.animate(cameraUpdate, durationMs = 800)
                }
            }
        }
    }

    val properties = MapProperties(isMyLocationEnabled = true)
    val uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false)
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = properties,
        uiSettings = uiSettings,
        contentPadding = PaddingValues(bottom = bottomPadding)
    ){
        vehicles.forEach { vehicle ->
            vehicle.lastLocation?.let { loc ->
                Marker(
                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = vehicle.name, // Aparece al pulsar el pin
                    snippet = vehicle.model
                )
            }
        }
    }

}