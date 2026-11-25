package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.ui.theme.LocalIsDarkTheme
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.Flow
import platform.UIKit.UIView

data class IosMapMarker(val id: String, val lat: Double, val lng: Double, val title: String, val vehicleIndex:Int, val selectedVehicleIndex: Int)
var iosMapViewFactory: (() -> UIView)? = null
var updateMapPadding: ((Double) -> Unit)? = null

/**
 * El JSON del estilo oscuro, o `null` para el estilo normal. Lo aplica Swift, que es quien tiene el
 * `GMSMapView`: aquí no hay un equivalente a `MapProperties` de maps-compose.
 */
var updateMapStyle: ((String?) -> Unit)? = null

/**
 * Mueve la cámara: latitud, longitud, zoom —`null` para conservar el que haya— y si se anima. Lo
 * decide la regla común (spec 017); Swift solo lo ejecuta, y si el mapa aún no existe lo guarda para
 * que nazca ya ahí.
 */
var iosMapUpdateCamera: ((Double, Double, Double?, Boolean) -> Unit)? = null
var iosMapUpdateMarkers: ((List<IosMapMarker>) -> Unit)? = null

var iosOnMarkerDragged: ((String, Double, Double) -> Unit)? = null
var iosOnMarkerTapped: ((String) -> Unit)? = null
var iosOnMapLongPressed: ((Double, Double) -> Unit)? = null
var iosOnMapReleased: ((UIView) -> Unit)? = null
@OptIn(ExperimentalForeignApi::class)
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
    // El tema de la app, no el del sistema — igual que en Android, porque el selector de Cuenta
    // puede tenerlos en desacuerdo.
    val isDarkTheme = LocalIsDarkTheme.current

    //Avisar a Swift de los marcadores
    LaunchedEffect(vehicles, selectedVehicleIndex) {
        val markers = vehicles.mapIndexedNotNull { index, vehicle ->
            vehicle.lastLocation?.let { loc ->
                IosMapMarker(vehicle.id, loc.latitude, loc.longitude, vehicle.name, index, selectedVehicleIndex)
            }
        }
        iosMapUpdateMarkers?.invoke(markers)
    }

    // La cámara solo hace lo que le mandan (spec 017): la regla vive en código común.
    LaunchedEffect(cameraRequests) {
        cameraRequests.collect { request ->
            iosMapUpdateCamera?.invoke(
                request.point.latitude,
                request.point.longitude,
                request.zoom?.toDouble(),
                request.animated
            )
        }
    }

    // El estilo se manda por LaunchedEffect, como todo lo demás que este fichero le dice a Swift.
    // Ponerlo solo en el `update` de UIKitView aplicaba bien el oscuro al crearse el mapa, pero no
    // lo retiraba al pasar a claro: esa lambda captura `isDarkTheme` por valor, y que se reejecute
    // al recomponer no es algo en lo que convenga confiar. La llamada del `update` se queda, porque
    // cubre el caso contrario —el mapa puede crearse durante la medición, después de que este
    // efecto ya haya corrido—; mandar dos veces el mismo estilo no hace nada.
    LaunchedEffect(isDarkTheme) {
        updateMapStyle?.invoke(if (isDarkTheme) darkMapStyleJson else null)
    }

    LaunchedEffect(onMarkerDragged, onMarkerTapped, onMapLongPressed) {
        iosOnMarkerDragged = { id, lat, lng -> onMarkerDragged(id, lat, lng) }
        iosOnMarkerTapped = { id -> onMarkerTapped(id) }
        iosOnMapLongPressed = { lat, lng -> onMapLongPressed(lat, lng) }
    }

    UIKitView(
        factory = {
            // Llamamos a la función que Swift nos proporcionó
            iosMapViewFactory?.invoke() ?: UIView()
        },
        update = {
            updateMapStyle?.invoke(if (isDarkTheme) darkMapStyleJson else null)

            // Sin restarle nada, igual que Android: `bottomPadding` ya es la altura del bloque
            // de tarjetas medida con su propio padding, así que es justo lo que hay que dejar
            // libre. El -30.0 que había compensaba que el mapa se dibujaba por detrás de la barra
            // de pestañas, cuando la barra vivía dentro de esta pantalla; desde la spec 009 la
            // barra está fuera y el mapa ya no llega hasta ahí, así que restar tapaba el logo.
            updateMapPadding?.invoke(bottomPadding.value.toDouble())
        },
        // Al irse el mapa —al cambiar de pestaña—, Swift lo suelta. Si no, una orden de cámara que
        // llegue antes de que exista el siguiente iría al viejo y se perdería; sin mapa, se guarda
        // para que el nuevo nazca ya en su sitio (spec 017).
        onRelease = { view -> iosOnMapReleased?.invoke(view) },
        modifier = modifier
    )
}