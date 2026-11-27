package com.albertomedina.apark.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.albertomedina.apark.ui.theme.LocalIsDarkTheme
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.parkedAtPlace
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest


@Composable
actual fun AparKMap(
    modifier: Modifier,
    bottomPadding: Dp,
    vehicles: List<Vehicle>,
    selectedVehicleIndex: Int,
    cameraRequests: Flow<CameraRequest>,
    onMarkerDragged: (String, Double, Double) -> Unit,
    onMarkerTapped: (String) -> Unit,
    onMapLongPressed: (Double, Double) -> Unit
) {

    val context = LocalContext.current
    val density = LocalDensity.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(DefaultMapCenter.latitude, DefaultMapCenter.longitude),
            DEFAULT_MAP_ZOOM
        )
    }

    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // La cámara solo hace lo que le mandan (spec 017): la regla vive en código común. `collectLatest`
    // corta el vuelo anterior si llega otra orden, que es lo que se quiere al deslizar rápido.
    LaunchedEffect(cameraRequests) {
        cameraRequests.collectLatest { request ->
            val target = LatLng(request.point.latitude, request.point.longitude)
            val update = request.zoom?.let { CameraUpdateFactory.newLatLngZoom(target, it) }
                ?: CameraUpdateFactory.newLatLng(target)
            if (request.animated) {
                try {
                    cameraPositionState.animate(update, durationMs = 800)
                } catch (e: CancellationException) {
                    // Tocar el mapa a mitad de vuelo lo interrumpe con una cancelación que no es
                    // nuestra: sin capturarla, terminaría este recolector y la cámara dejaría de
                    // obedecer. Si la cancelación sí es nuestra —otra orden, o el mapa se va—, sigue.
                    currentCoroutineContext().ensureActive()
                }
            } else {
                cameraPositionState.move(update)
            }
        }
    }

    val uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false)
    // El tema de la app, no el del sistema: con el selector de Cuenta los dos pueden discrepar.
    val isDarkMode = LocalIsDarkTheme.current

    val mapProperties = remember(isDarkMode, hasFine) {
        MapProperties(
            isMyLocationEnabled = hasFine,
            mapStyleOptions = if (isDarkMode) MapStyleOptions(darkMapStyleJson) else null
        )
    }

    // Al volver a la pestaña del mapa, Android **reconstruye el MapView**: es una View nativa que
    // se destruye al salir de la composición, y rehacerla reinicia la superficie GL y vuelve a
    // pedir teselas. iOS no lo enseña porque su estado vive en Swift, fuera de Compose, en
    // `MapLocationController.shared`. La diferencia es de plataforma y está en apps ajenas también.
    //
    // La cámara no es el problema —al volver, el ViewModel manda un salto al vehículo
    // (`HomeEvent.MapShown`)—, así que izar su estado no arreglaría nada. Lo único que se ve es el
    // hueco mientras el mapa dibuja su primer fotograma, y lo que se hace con eso es taparlo.
    var isMapLoaded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings,
            contentPadding = PaddingValues(bottom = bottomPadding),
            onMapLoaded = { isMapLoaded = true },
            // Mantener pulsado un pin no es «aparcar aquí» (spec 024): el del elegido se arrastra, y el
            // de otro no se mueve. Sin esto, mantener pulsado el pin de otro vehículo llevaba el
            // elegido encima de él, porque en Android esa pulsación también llega al mapa.
            onMapLongClick = { latLng ->
                val projection = cameraPositionState.projection
                val pressed = projection?.toScreenLocation(latLng)
                val onPin = pressed != null && vehicles.any { vehicle ->
                    val loc = vehicle.lastLocation ?: return@any false
                    val pin = projection.toScreenLocation(LatLng(loc.latitude, loc.longitude))
                    isOnPin(pin.x.toFloat(), pin.y.toFloat(), pressed.x.toFloat(), pressed.y.toFloat(), density.density)
                }
                if (!onPin) onMapLongPressed(latLng.latitude, latLng.longitude)
            }
        ) {
            vehicles.forEachIndexed { index, vehicle ->
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

                    val selected = index == selectedVehicleIndex
                    val atPlace = vehicle.parkedAtPlace != null

                    // Un pin propio y no el del SDK (spec 024): `defaultMarker` es un BitmapDescriptor
                    // opaco, sin forma de dibujarle encima la casa del garaje. Así el pin normal y el
                    // de la casa tienen la misma forma. Las claves rehacen la imagen al cambiar.
                    MarkerComposable(
                        selected, atPlace,
                        state = markerState,
                        title = vehicle.name,
                        anchor = VehiclePinAnchor,
                        // Solo se arrastra el del vehículo elegido (spec 024): para mover otro, se
                        // elige antes, en el carrusel o tocando su pin.
                        draggable = selected,
                        // Tocarlo elige el vehículo. `true` evita lo de por defecto —centrarlo y abrir
                        // un globo—: la cámara ya va a él por la regla común.
                        onClick = {
                            onMarkerTapped(vehicle.id)
                            true
                        }
                    ) {
                        VehiclePin(selected = selected, atPlace = atPlace)
                    }
                }
            }
        }


        AnimatedVisibility(
            visible = !isMapLoaded,
            // Aparece de golpe y se va con un fundido: el velo tiene que estar ya puesto en el
            // primer fotograma, que es justo el que queremos tapar.
            enter = EnterTransition.None,
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
