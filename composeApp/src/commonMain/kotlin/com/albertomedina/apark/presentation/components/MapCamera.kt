package com.albertomedina.apark.presentation.components

import com.albertomedina.apark.domain.model.Vehicle

/**
 * La cámara del mapa (spec 017): **una sola regla para las dos plataformas**, decidida aquí y no en
 * cada `AparKMap`. El mapa de cada plataforma solo ejecuta las órdenes ([CameraRequest]); quién y
 * cuándo se mueve lo decide [cameraMoveFor].
 */

/** El zoom de calle: el de un vehículo elegido y el de tu ubicación, igual en Android y en iOS. */
const val STREET_ZOOM = 17f

/**
 * De cerca, al nivel de los portales: el zoom al aparcar o mover el vehículo elegido, y el de «Ir al
 * vehículo». El GPS se equivoca a veces varios metros, y de lejos el error no se ve; de cerca sí, y
 * se corrige a mano arrastrando el marcador o manteniendo pulsado el sitio bueno.
 */
const val CLOSE_ZOOM = 20f

data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * Lo que enseña el mapa: el vehículo elegido y dónde está, `null` si nunca se aparcó. Sin vehículo
 * —la tarjeta «+», o no hay ninguno—, `vehicleId` es `null`.
 */
data class MapFocus(val vehicleId: String?, val location: GeoPoint?)

sealed interface CameraMove {
    /** A un punto. Sin [zoom], se conserva el que haya. Sin animar, es un salto. */
    data class ToPoint(val point: GeoPoint, val zoom: Float?, val animated: Boolean) : CameraMove

    /** A la ubicación de quien mira, que hay que averiguar antes de poder moverse. */
    data class ToUser(val animated: Boolean) : CameraMove
}

/** Una orden ya resuelta, lista para el mapa de cada plataforma. */
data class CameraRequest(val point: GeoPoint, val zoom: Float?, val animated: Boolean)

/** Dónde está un vehículo para el mapa: su última ubicación, si tiene una de verdad. */
val Vehicle.mapLocation: GeoPoint?
    get() = lastLocation?.takeIf { it.timestamp != 0L }?.let { GeoPoint(it.latitude, it.longitude) }

/**
 * Cómo se mueve la cámara al pasar de [previous] a [current]. `null`: no se mueve.
 *
 * - **[previous] `null`**: el mapa acaba de aparecer. Un salto, no un vuelo, al vehículo o a ti.
 * - **Otro vehículo**: vuela a él, o a ti si nunca se aparcó, que es donde lo dejaría «Aparcar».
 * - **El mismo vehículo en otro sitio**: lo sigue de cerca ([CLOSE_ZOOM]), para ver si el GPS acertó.
 * - **Todo lo demás no la mueve**: que otro miembro aparque otro coche, que cambie un nombre o que se
 *   reordene la lista. Antes cualquier cambio en la lista devolvía la cámara al vehículo.
 */
fun cameraMoveFor(previous: MapFocus?, current: MapFocus): CameraMove? {
    val location = current.location
    return when {
        previous == null ->
            location?.let { CameraMove.ToPoint(it, STREET_ZOOM, animated = false) } ?: CameraMove.ToUser(animated = false)
        // La tarjeta «+»: no hay nada nuevo que enseñar.
        current.vehicleId == null -> null
        current.vehicleId != previous.vehicleId ->
            location?.let { CameraMove.ToPoint(it, STREET_ZOOM, animated = true) } ?: CameraMove.ToUser(animated = true)
        location != null && location != previous.location -> CameraMove.ToPoint(location, CLOSE_ZOOM, animated = true)
        else -> null
    }
}
