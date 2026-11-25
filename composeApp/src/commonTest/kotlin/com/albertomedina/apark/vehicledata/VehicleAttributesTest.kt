package com.albertomedina.apark.vehicledata

import com.albertomedina.apark.domain.model.EmissionsLabel
import com.albertomedina.apark.domain.model.Fuel
import com.albertomedina.apark.domain.model.Powertrain
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.VehicleColor
import com.albertomedina.apark.domain.model.VehicleType
import com.albertomedina.apark.domain.model.details
import com.albertomedina.apark.domain.model.fuelFor
import com.albertomedina.apark.domain.model.hasDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VehicleAttributesTest {

    @Test
    fun `cada código conocido vuelve a su valor`() {
        VehicleType.entries.forEach { assertEquals(it, VehicleType.fromCode(it.code)) }
        Powertrain.entries.forEach { assertEquals(it, Powertrain.fromCode(it.code)) }
        Fuel.entries.forEach { assertEquals(it, Fuel.fromCode(it.code)) }
        VehicleColor.entries.forEach { assertEquals(it, VehicleColor.fromCode(it.code)) }
        EmissionsLabel.entries.forEach { assertEquals(it, EmissionsLabel.fromCode(it.code)) }
    }

    @Test
    fun `sin dato no es otro`() {
        assertNull(VehicleType.fromCode(""))
        assertNull(Powertrain.fromCode(""))
        assertNull(Fuel.fromCode(""))
        assertNull(VehicleColor.fromCode(""))
        assertNull(EmissionsLabel.fromCode(""))
    }

    @Test
    fun `un código que esta versión no conoce sale como otro`() {
        assertEquals(VehicleType.OTHER, VehicleType.fromCode("tractor"))
        assertEquals(Powertrain.OTHER, Powertrain.fromCode("nuclear"))
        assertEquals(Fuel.OTHER, Fuel.fromCode("ethanol"))
    }

    @Test
    fun `un color o una etiqueta desconocidos no se inventan`() {
        assertNull(VehicleColor.fromCode("granate"))
        assertNull(VehicleColor.fromCode("other"))
        assertNull(EmissionsLabel.fromCode("crit_air_1"))
    }

    @Test
    fun `un eléctrico no lleva combustible`() {
        assertEquals("", fuelFor(Powertrain.ELECTRIC, "petrol"))
        assertFalse(Powertrain.ELECTRIC.asksFuel)
        assertTrue(Powertrain.ELECTRIC.allowedFuels.isEmpty())
    }

    @Test
    fun `uno de pila de combustible es de hidrógeno sin preguntar`() {
        assertEquals("hydrogen", fuelFor(Powertrain.FUEL_CELL, ""))
        assertEquals("hydrogen", fuelFor(Powertrain.FUEL_CELL, "diesel"))
        assertFalse(Powertrain.FUEL_CELL.asksFuel)
    }

    @Test
    fun `un híbrido es de gasolina o de diésel`() {
        listOf(Powertrain.MILD_HYBRID, Powertrain.HYBRID, Powertrain.PLUG_IN_HYBRID).forEach {
            assertEquals(listOf(Fuel.PETROL, Fuel.DIESEL), it.allowedFuels)
            assertEquals("diesel", fuelFor(it, "diesel"))
            assertEquals("", fuelFor(it, "lpg"))
        }
    }

    @Test
    fun `al cambiar de sistema se conserva el combustible si sigue teniendo sentido`() {
        assertEquals("lpg", fuelFor(Powertrain.COMBUSTION, "lpg"))
        assertEquals("", fuelFor(Powertrain.COMBUSTION, "hydrogen"))
        assertEquals("petrol", fuelFor(Powertrain.HYBRID, "petrol"))
    }

    @Test
    fun `sin sistema el combustible va por su cuenta`() {
        assertEquals("diesel", fuelFor(null, "diesel"))
        assertEquals("", fuelFor(null, ""))
    }

    @Test
    fun `los datos editables salen del vehículo tal cual aunque el código sea desconocido`() {
        val vehicle = Vehicle(id = "v", name = "Coche", licensePlate = "1234ABC", color = "granate", make = "Škoda")
        val details = vehicle.details()
        assertEquals("granate", details.color)
        assertEquals("Škoda", details.make)
        assertEquals("1234ABC", details.licensePlate)
    }

    @Test
    fun `un vehículo de antes no tiene datos`() {
        assertFalse(Vehicle(id = "v", name = "Coche", licensePlate = "1234ABC").hasDetails)
        assertTrue(Vehicle(id = "v", name = "Coche", color = "red").hasDetails)
    }

    @Test
    fun `un color desconocido no cuenta como dato`() {
        assertFalse(Vehicle(id = "v", name = "Coche", color = "granate").hasDetails)
        assertFalse(Vehicle(id = "v", name = "Coche", emissionsLabel = "crit_air_1").hasDetails)
    }
}
