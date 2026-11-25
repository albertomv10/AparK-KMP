package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.albertomedina.apark.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

/**
 * El mapa de cada plataforma. **No decide la cámara** (spec 017): ejecuta las órdenes que llegan por
 * [cameraRequests], que salen de una sola regla en código común ([cameraMoveFor]). Así Android e iOS
 * se mueven igual.
 */
@Composable
expect fun AparKMap(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    vehicles: List<Vehicle>,
    selectedVehicleIndex: Int,
    cameraRequests: Flow<CameraRequest>,
    /** Se ha arrastrado el marcador de un vehículo a otro sitio. */
    onMarkerDragged: (vehicleId: String, latitude: Double, longitude: Double) -> Unit,
    /** Se ha tocado el marcador de un vehículo: se elige ese vehículo. */
    onMarkerTapped: (vehicleId: String) -> Unit,
    /** Se ha mantenido pulsado un punto del mapa vacío: el vehículo elegido pasa ahí. */
    onMapLongPressed: (latitude: Double, longitude: Double) -> Unit
)

/** Dónde nace la cámara antes de la primera orden: España entera, igual en las dos plataformas. */
val DefaultMapCenter = GeoPoint(40.4168, -3.7038)
const val DEFAULT_MAP_ZOOM = 5f
