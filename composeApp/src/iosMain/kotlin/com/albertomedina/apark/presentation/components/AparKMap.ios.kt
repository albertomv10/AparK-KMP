package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.ui.theme.LocalIsDarkTheme
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.UIKit.UIView

data class IosMapMarker(val id: String, val lat: Double, val lng: Double, val title: String, val vehicleIndex:Int, val selectedVehicleIndex: Int)
var iosMapViewFactory: (() -> UIView)? = null
var updateMapPadding: ((Double) -> Unit)? = null

/**
 * El JSON del estilo oscuro, o `null` para el estilo normal. Lo aplica Swift, que es quien tiene el
 * `GMSMapView`: aquí no hay un equivalente a `MapProperties` de maps-compose.
 */
var updateMapStyle: ((String?) -> Unit)? = null

var iosMapUpdateCamera: ((Double, Double, Double, Boolean) -> Unit)? = null
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
    // El tema de la app, no el del sistema — igual que en Android, porque el selector de Cuenta
    // puede tenerlos en desacuerdo.
    val isDarkTheme = LocalIsDarkTheme.current

    var isFirstLoad by remember { mutableStateOf(true) }

    // Variables de estado para detectar cambios de posición vs cambios de tarjeta
    var lastVehicleId by remember { mutableStateOf<String?>(null) }
    var lastProcessedLocation by remember { mutableStateOf<Vehicle.LocationModel?>(null) }

    //Avisar a Swift de los marcadores
    LaunchedEffect(vehicles, selectedVehicleIndex) {
        val markers = vehicles.mapIndexedNotNull { index, vehicle ->
            vehicle.lastLocation?.let { loc ->
                IosMapMarker(vehicle.id, loc.latitude, loc.longitude, vehicle.name, index, selectedVehicleIndex)
            }
        }
        iosMapUpdateMarkers?.invoke(markers)
    }

    //Avisar a Swift para mover la cámara
    LaunchedEffect(selectedVehicleIndex, vehicles) {
        if (vehicles.isNotEmpty() && selectedVehicleIndex < vehicles.size) {
            val currentVehicle = vehicles[selectedVehicleIndex]
            val currentLocation = currentVehicle.lastLocation

            if (currentLocation != null) {
                // Lógica de detección: ¿Es el mismo coche pero se ha movido?
                val isSameVehicle = lastVehicleId == currentVehicle.id
                val locationChanged = lastProcessedLocation != null &&
                        (lastProcessedLocation!!.latitude != currentLocation.latitude ||
                                lastProcessedLocation!!.longitude != currentLocation.longitude)

                // 20.0 si es actualización/arrastre, 17.0 si es un cambio de tarjeta (swipe)
                val targetZoom =
                    if (isSameVehicle && locationChanged && !isFirstLoad) 20.0 else 17.0

                // Invocamos a Swift pasando el nuevo parámetro de zoom
                // Nota: Asegúrate de que iosMapUpdateCamera acepte Double como 3er parámetro
                iosMapUpdateCamera?.invoke(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    targetZoom,
                    !isFirstLoad
                )

                // Actualizamos referencias
                isFirstLoad = false
                lastProcessedLocation = currentLocation
                lastVehicleId = currentVehicle.id
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

    // El estilo se manda por LaunchedEffect, como todo lo demás que este fichero le dice a Swift.
    // Ponerlo solo en el `update` de UIKitView aplicaba bien el oscuro al crearse el mapa, pero no
    // lo retiraba al pasar a claro: esa lambda captura `isDarkTheme` por valor, y que se reejecute
    // al recomponer no es algo en lo que convenga confiar. La llamada del `update` se queda, porque
    // cubre el caso contrario —el mapa puede crearse durante la medición, después de que este
    // efecto ya haya corrido—; mandar dos veces el mismo estilo no hace nada.
    LaunchedEffect(isDarkTheme) {
        updateMapStyle?.invoke(if (isDarkTheme) darkMapStyleJson else null)
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
            updateMapStyle?.invoke(if (isDarkTheme) darkMapStyleJson else null)

            // Sin restarle nada, igual que Android: `bottomPadding` ya es la altura del bloque
            // de tarjetas medida con su propio padding, así que es justo lo que hay que dejar
            // libre. El -30.0 que había compensaba que el mapa se dibujaba por detrás de la barra
            // de pestañas, cuando la barra vivía dentro de esta pantalla; desde la spec 009 la
            // barra está fuera y el mapa ya no llega hasta ahí, así que restar tapaba el logo.
            updateMapPadding?.invoke(bottomPadding.value.toDouble())
        },
        modifier = modifier
    )
}