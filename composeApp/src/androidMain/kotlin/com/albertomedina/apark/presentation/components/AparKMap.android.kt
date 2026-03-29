package com.albertomedina.apark.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.core.content.ContextCompat
import com.albertomedina.apark.domain.model.Vehicle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

// Estilo "Night" oficial de Google Maps
private val darkMapStyleJson = """
    [
      { "elementType": "geometry", "stylers": [{ "color": "#242f3e" }] },
      { "elementType": "labels.text.stroke", "stylers": [{ "color": "#242f3e" }] },
      { "elementType": "labels.text.fill", "stylers": [{ "color": "#746855" }] },
      { "featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{ "color": "#d59563" }] },
      { "featureType": "poi", "elementType": "labels.text.fill", "stylers": [{ "color": "#d59563" }] },
      { "featureType": "poi.park", "elementType": "geometry", "stylers": [{ "color": "#263c3f" }] },
      { "featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{ "color": "#6b9a76" }] },
      { "featureType": "road", "elementType": "geometry", "stylers": [{ "color": "#38414e" }] },
      { "featureType": "road", "elementType": "geometry.stroke", "stylers": [{ "color": "#212a37" }] },
      { "featureType": "road", "elementType": "labels.text.fill", "stylers": [{ "color": "#9ca5b3" }] },
      { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#746855" }] },
      { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{ "color": "#1f2835" }] },
      { "featureType": "road.highway", "elementType": "labels.text.fill", "stylers": [{ "color": "#f3d19c" }] },
      { "featureType": "transit", "elementType": "geometry", "stylers": [{ "color": "#2f3948" }] },
      { "featureType": "transit.station", "elementType": "labels.text.fill", "stylers": [{ "color": "#d59563" }] },
      { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#17263c" }] },
      { "featureType": "water", "elementType": "labels.text.fill", "stylers": [{ "color": "#515c6d" }] },
      { "featureType": "water", "elementType": "labels.text.stroke", "stylers": [{ "color": "#17263c" }] }
    ]
""".trimIndent()

@Composable
actual fun AparKMap(
    modifier: Modifier,
    bottomPadding: Dp,
    vehicles: List<Vehicle>,
    selectedVehicleIndex: Int,
    centerCameraTrigger: Int,
    onMarkerDragged: (String, Double, Double) -> Unit
) {

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val cameraPositionState = rememberCameraPositionState()
    var isFirstLoad by remember { mutableStateOf(true) }

    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(vehicles.size) {
        if (vehicles.isEmpty()) {
            delay(200)
            val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (hasFineLocation || hasCoarseLocation) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                            LatLng(location.latitude, location.longitude),
                            15f
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(centerCameraTrigger) {
        if (centerCameraTrigger > 0) {


            if (hasFine || hasCoarse) {
                try {
                    val location = fusedLocationClient.lastLocation.await()

                    if (location != null) {

                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude), 15f
                            ),
                            durationMs = 800
                        )
                    }
                } catch (e: Exception) {
                    // Manejar si algo falla con el GPS
                }
            }
        }
    }

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

    val uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false)
    val isDarkMode = isSystemInDarkTheme()

    val mapProperties = remember(isDarkMode) {
        MapProperties(
            isMyLocationEnabled = hasFine,
            mapStyleOptions = if (isDarkMode) MapStyleOptions(darkMapStyleJson) else null
        )
    }
    val markers = listOf(
        BitmapDescriptorFactory.HUE_AZURE,
        BitmapDescriptorFactory.HUE_GREEN,
        BitmapDescriptorFactory.HUE_YELLOW,
        BitmapDescriptorFactory.HUE_MAGENTA,
        BitmapDescriptorFactory.HUE_ORANGE,
        BitmapDescriptorFactory.HUE_RED,
        BitmapDescriptorFactory.HUE_ROSE,
        BitmapDescriptorFactory.HUE_BLUE,
        BitmapDescriptorFactory.HUE_CYAN,
        BitmapDescriptorFactory.HUE_VIOLET,
        BitmapDescriptorFactory.HUE_ROSE,
    )

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        contentPadding = PaddingValues(bottom = bottomPadding)
    ){
        vehicles.forEach { vehicle ->
            vehicle.lastLocation?.let { loc ->

                val markerState = remember(vehicle.id) {
                    MarkerState(position = LatLng(loc.latitude, loc.longitude))
                }

                LaunchedEffect(loc) {
                    if (!markerState.isDragging) {
                        markerState.position = LatLng(loc.latitude, loc.longitude)
                    }
                }

                LaunchedEffect(markerState.isDragging) {
                    if (!markerState.isDragging) {
                        val hasMoved = markerState.position.latitude != loc.latitude ||
                                markerState.position.longitude != loc.longitude

                        if (hasMoved) {
                            onMarkerDragged(
                                vehicle.id,
                                markerState.position.latitude,
                                markerState.position.longitude
                            )
                        }
                    }
                }
                val icon = if (vehicles.indexOf(vehicle) >= markers.size) {
                    BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                } else {
                    BitmapDescriptorFactory.defaultMarker(markers[vehicles.indexOf(vehicle)])
                }

                Marker(
                    state = markerState,
                    title = vehicle.name,
                    snippet = vehicle.model,
                    icon = icon,
                    draggable = true
                )
            }
        }
    }

}