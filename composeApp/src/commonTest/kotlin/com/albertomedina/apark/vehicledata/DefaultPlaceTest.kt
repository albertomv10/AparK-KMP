package com.albertomedina.apark.vehicledata

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.parkedAtPlace
import com.albertomedina.apark.domain.usecase.SetDefaultPlaceUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Spec 024: cuándo está un vehículo en su ubicación predeterminada, y con qué nombre se guarda. */
class DefaultPlaceTest {

    private val garaje = Vehicle.DefaultPlace(name = "Garaje", latitude = 40.41, longitude = -3.7)

    private fun parked(atPlace: Boolean) =
        Vehicle.LocationModel(latitude = 40.41, longitude = -3.7, timestamp = 1, atDefaultPlace = atPlace)

    @Test
    fun `aparcado con su botón está en su ubicación`() {
        assertEquals(garaje, Vehicle(defaultPlace = garaje, lastLocation = parked(true)).parkedAtPlace)
    }

    @Test
    fun `aparcado de otra forma no lo está aunque tenga ubicación`() {
        assertNull(Vehicle(defaultPlace = garaje, lastLocation = parked(false)).parkedAtPlace)
        assertNull(Vehicle(defaultPlace = garaje, lastLocation = null).parkedAtPlace)
    }

    @Test
    fun `si quitan la ubicación con el coche dentro ya no hay sitio que nombrar`() {
        assertNull(Vehicle(defaultPlace = null, lastLocation = parked(true)).parkedAtPlace)
    }

    @Test
    fun `el nombre se guarda sin espacios en los bordes y como mucho de 40`() {
        assertEquals("Plaza 23", SetDefaultPlaceUseCase.placeName("  Plaza 23 ", "Garaje"))
        assertEquals(40, SetDefaultPlaceUseCase.placeName("x".repeat(50), "Garaje").length)
    }

    @Test
    fun `sin nombre se llama como proponía el campo`() {
        assertEquals("Garaje", SetDefaultPlaceUseCase.placeName("   ", "Garaje"))
        assertEquals("Garage", SetDefaultPlaceUseCase.placeName("", "Garage"))
    }
}
