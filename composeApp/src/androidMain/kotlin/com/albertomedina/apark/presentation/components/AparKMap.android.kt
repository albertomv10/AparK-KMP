package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
actual fun AparKMap(modifier: Modifier) {

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Este estado controla dónde está mirando la cámara
    val cameraPositionState = rememberCameraPositionState()

    // LaunchedEffect se ejecuta una sola vez cuando el Composable se carga
    LaunchedEffect(Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Si encontramos la ubicación, movemos la cámara con un zoom de 15 (nivel de calles)
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    15f
                )
            }
        }
    }

    val properties = MapProperties(isMyLocationEnabled = true)
    val uiSettings = MapUiSettings(myLocationButtonEnabled = false)
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = properties,
        uiSettings = uiSettings
    )

}