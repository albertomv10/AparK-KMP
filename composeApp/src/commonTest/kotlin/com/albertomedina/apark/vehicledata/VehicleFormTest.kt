package com.albertomedina.apark.vehicledata

import com.albertomedina.apark.domain.model.VehicleDetails
import com.albertomedina.apark.presentation.vehicleform.NAME_MAX_LENGTH
import com.albertomedina.apark.presentation.vehicleform.VehicleFormEvent
import com.albertomedina.apark.presentation.vehicleform.VehicleFormState
import com.albertomedina.apark.presentation.vehicleform.autoName
import com.albertomedina.apark.presentation.vehicleform.reduce
import com.albertomedina.apark.presentation.vehicleform.toSave
import com.albertomedina.apark.presentation.vehicleform.withNameError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VehicleFormTest {

    private fun VehicleFormState.after(vararg events: VehicleFormEvent) = events.fold(this) { s, e -> reduce(s, e) }

    @Test
    fun `el nombre se rellena con la marca y el modelo`() {
        val state = VehicleFormState().after(
            VehicleFormEvent.MakeChanged("Škoda"),
            VehicleFormEvent.ModelChanged("Octavia")
        )
        assertEquals("Škoda Octavia", state.details.name)
    }

    @Test
    fun `el nombre automático sigue a la marca mientras se escribe`() {
        val state = VehicleFormState().after(
            VehicleFormEvent.MakeChanged("S"),
            VehicleFormEvent.MakeChanged("Se"),
            VehicleFormEvent.MakeChanged("Seat")
        )
        assertEquals("Seat", state.details.name)
    }

    @Test
    fun `un nombre escrito a mano no se pisa`() {
        val state = VehicleFormState().after(
            VehicleFormEvent.MakeChanged("Škoda"),
            VehicleFormEvent.ModelChanged("Octavia"),
            VehicleFormEvent.NameChanged("El de papá"),
            VehicleFormEvent.ModelChanged("Fabia")
        )
        assertEquals("El de papá", state.details.name)
    }

    @Test
    fun `al editar un nombre que era el automático sigue siéndolo`() {
        assertEquals("Škoda Fabia", autoName("Škoda Octavia", "Škoda", "Octavia", "Škoda", "Fabia"))
        assertEquals("Mi coche", autoName("Mi coche", "Škoda", "Octavia", "Škoda", "Fabia"))
    }

    @Test
    fun `borrar el nombre lo devuelve al automático`() {
        val state = VehicleFormState().after(
            VehicleFormEvent.MakeChanged("Seat"),
            VehicleFormEvent.NameChanged("Otro"),
            VehicleFormEvent.NameChanged(""),
            VehicleFormEvent.ModelChanged("Ibiza")
        )
        assertEquals("Seat Ibiza", state.details.name)
    }

    @Test
    fun `elegir otra vez lo mismo lo quita`() {
        val state = VehicleFormState().after(
            VehicleFormEvent.TypeSelected("car"),
            VehicleFormEvent.ColorSelected("red"),
            VehicleFormEvent.ColorSelected("red")
        )
        assertEquals("car", state.details.type)
        assertEquals("", state.details.color)
    }

    @Test
    fun `pasar a eléctrico borra el combustible y a pila de combustible pone hidrógeno`() {
        val electric = VehicleFormState().after(
            VehicleFormEvent.FuelSelected("petrol"),
            VehicleFormEvent.PowertrainSelected("electric")
        )
        assertEquals("", electric.details.fuel)
        val fuelCell = electric.after(VehicleFormEvent.PowertrainSelected("fuel_cell"))
        assertEquals("hydrogen", fuelCell.details.fuel)
    }

    @Test
    fun `pasar a híbrido conserva el diésel y quita el GLP`() {
        val diesel = VehicleFormState().after(VehicleFormEvent.FuelSelected("diesel"), VehicleFormEvent.PowertrainSelected("hybrid"))
        assertEquals("diesel", diesel.details.fuel)
        val lpg = VehicleFormState().after(VehicleFormEvent.FuelSelected("lpg"), VehicleFormEvent.PowertrainSelected("hybrid"))
        assertEquals("", lpg.details.fuel)
    }

    @Test
    fun `los campos no pasan de lo que admiten las reglas`() {
        val state = VehicleFormState().after(
            VehicleFormEvent.NameChanged("x".repeat(100)),
            VehicleFormEvent.PlateChanged("1".repeat(30)),
            VehicleFormEvent.MakeChanged("m".repeat(50))
        )
        assertEquals(NAME_MAX_LENGTH, state.details.name.length)
        assertEquals(15, state.details.licensePlate.length)
        assertEquals(40, state.details.make.length)
    }

    @Test
    fun `sin nombre no se guarda`() {
        assertNull(VehicleFormState(details = VehicleDetails(name = "   ")).toSave())
    }

    @Test
    fun `al guardar se limpian los bordes y la matrícula va en mayúsculas`() {
        val saved = VehicleFormState(
            details = VehicleDetails(name = " Coche ", licensePlate = " 1234abc ", make = " Seat ", model = "Ibiza ", color = "red")
        ).toSave()
        assertEquals(VehicleDetails(name = "Coche", licensePlate = "1234ABC", make = "Seat", model = "Ibiza", color = "red"), saved)
    }

    @Test
    fun `escribir el nombre quita el error`() {
        val withError = VehicleFormState().withNameError()
        assertTrue(withError.nameError)
        assertFalse(reduce(withError, VehicleFormEvent.NameChanged("Coche")).nameError)
        assertFalse(reduce(withError, VehicleFormEvent.MakeChanged("Seat")).nameError)
    }
}
