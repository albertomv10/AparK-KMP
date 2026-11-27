package com.albertomedina.apark.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.albertomedina.apark.ui.theme.LocalIsDarkTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
actual fun PlacePickerMap(
    modifier: Modifier,
    initial: GeoPoint,
    initialZoom: Float,
    onCenterChanged: (GeoPoint) -> Unit
) {
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(initial.latitude, initial.longitude), initialZoom)
    }
    val currentOnCenterChanged by rememberUpdatedState(onCenterChanged)

    // Solo cuando la cámara se para: mientras se arrastra, el centro cambia en cada fotograma.
    LaunchedEffect(cameraPositionState) {
        snapshotFlow { cameraPositionState.isMoving to cameraPositionState.position.target }
            .filter { (moving, _) -> !moving }
            .map { (_, target) -> GeoPoint(target.latitude, target.longitude) }
            .distinctUntilChanged()
            .collect { currentOnCenterChanged(it) }
    }

    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    // El tema de la app, no el del sistema, como en el mapa principal.
    val isDarkMode = LocalIsDarkTheme.current
    val properties = remember(isDarkMode, hasFine) {
        MapProperties(
            isMyLocationEnabled = hasFine,
            mapStyleOptions = if (isDarkMode) MapStyleOptions(darkMapStyleJson) else null
        )
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = properties,
        uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false)
    )
}
