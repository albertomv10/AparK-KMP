package com.albertomedina.apark.home

import com.albertomedina.apark.presentation.components.CLOSE_ZOOM
import com.albertomedina.apark.presentation.components.CameraMove
import com.albertomedina.apark.presentation.components.GeoPoint
import com.albertomedina.apark.presentation.components.MapFocus
import com.albertomedina.apark.presentation.components.STREET_ZOOM
import com.albertomedina.apark.presentation.components.cameraMoveFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapCameraTest {

    private val sol = GeoPoint(40.4169, -3.7035)
    private val mayor = GeoPoint(40.4155, -3.7074)
    private val furgoneta = MapFocus("furgoneta", sol)

    @Test
    fun `al aparecer el mapa salta al vehículo sin volar`() {
        assertEquals(CameraMove.ToPoint(sol, STREET_ZOOM, animated = false), cameraMoveFor(null, furgoneta))
    }

    @Test
    fun `al aparecer sin vehículo aparcado o sin vehículos salta a ti`() {
        assertEquals(CameraMove.ToUser(animated = false), cameraMoveFor(null, MapFocus("moto", null)))
        assertEquals(CameraMove.ToUser(animated = false), cameraMoveFor(null, MapFocus(null, null)))
    }

    @Test
    fun `al pasar a otro vehículo vuela a él con zoom de calle`() {
        assertEquals(CameraMove.ToPoint(mayor, STREET_ZOOM, animated = true), cameraMoveFor(furgoneta, MapFocus("coche", mayor)))
    }

    @Test
    fun `al pasar a uno que nunca se aparcó vuela a ti`() {
        assertEquals(CameraMove.ToUser(animated = true), cameraMoveFor(furgoneta, MapFocus("moto", null)))
    }

    @Test
    fun `si el elegido cambia de sitio lo sigue de cerca para ver si el GPS acertó`() {
        assertEquals(CameraMove.ToPoint(mayor, CLOSE_ZOOM, animated = true), cameraMoveFor(furgoneta, furgoneta.copy(location = mayor)))
    }

    @Test
    fun `un vehículo que se aparca por primera vez también se sigue`() {
        assertEquals(CameraMove.ToPoint(sol, CLOSE_ZOOM, animated = true), cameraMoveFor(MapFocus("moto", null), MapFocus("moto", sol)))
    }

    @Test
    fun `otros datos o reordenar no mueven la cámara`() {
        // Reordenar cambia el índice del elegido, pero no el foco: ni el vehículo ni su sitio.
        assertNull(cameraMoveFor(furgoneta, furgoneta.copy()))
    }

    @Test
    fun `la tarjeta de añadir no mueve la cámara y volver de ella sí`() {
        val plus = MapFocus(null, null)
        assertNull(cameraMoveFor(furgoneta, plus))
        assertEquals(CameraMove.ToPoint(sol, STREET_ZOOM, animated = true), cameraMoveFor(plus, furgoneta))
    }
}
