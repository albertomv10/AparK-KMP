package com.albertomedina.apark.incidents

import com.albertomedina.apark.domain.model.Fuel
import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.model.IncidentCategory
import com.albertomedina.apark.domain.model.IncidentType
import com.albertomedina.apark.domain.model.Powertrain
import com.albertomedina.apark.domain.model.incidentsView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class IncidentsTest {

    @Test
    fun `cada código vuelve a su valor`() {
        IncidentType.entries.forEach { assertEquals(it, IncidentType.fromCode(it.code)) }
    }

    @Test
    fun `un tipo desconocido o vacío se enseña como personalizada`() {
        assertEquals(IncidentType.CUSTOM, IncidentType.fromCode("engine"))
        assertEquals(IncidentType.CUSTOM, IncidentType.fromCode(""))
        assertEquals(IncidentType.CUSTOM, Incident(id = "i", type = "turbo").kind)
    }

    @Test
    fun `cada tipo es de una categoría salvo la personalizada`() {
        assertEquals(5, IncidentCategory.LEVELS.types.size)
        assertEquals(5, IncidentCategory.MECHANICS.types.size)
        assertEquals(3, IncidentCategory.TYRES.types.size)
        assertEquals(3, IncidentCategory.BODY.types.size)
        assertNull(IncidentType.CUSTOM.category)
        assertTrue(IncidentType.entries.filter { it != IncidentType.CUSTOM }.all { it.category != null })
    }

    @Test
    fun `sin datos del vehículo se ofrecen todos los tipos`() {
        assertTrue(IncidentType.entries.all { it.appliesTo(null, null) })
    }

    @Test
    fun `un eléctrico se carga y no reposta ni lleva aceite`() {
        assertTrue(IncidentType.LOW_CHARGE.appliesTo(Powertrain.ELECTRIC, null))
        assertFalse(IncidentType.LOW_FUEL.appliesTo(Powertrain.ELECTRIC, null))
        assertFalse(IncidentType.OIL.appliesTo(Powertrain.ELECTRIC, null))
        assertFalse(IncidentType.ADBLUE.appliesTo(Powertrain.ELECTRIC, null))
    }

    @Test
    fun `solo se carga lo que se enchufa`() {
        assertTrue(IncidentType.LOW_CHARGE.appliesTo(Powertrain.PLUG_IN_HYBRID, Fuel.PETROL))
        assertFalse(IncidentType.LOW_CHARGE.appliesTo(Powertrain.HYBRID, Fuel.PETROL))
        assertFalse(IncidentType.LOW_CHARGE.appliesTo(Powertrain.COMBUSTION, Fuel.DIESEL))
    }

    @Test
    fun `AdBlue solo en un diésel`() {
        assertTrue(IncidentType.ADBLUE.appliesTo(Powertrain.COMBUSTION, Fuel.DIESEL))
        assertFalse(IncidentType.ADBLUE.appliesTo(Powertrain.COMBUSTION, Fuel.PETROL))
        // Sin combustible indicado se ofrece, salvo en lo que no quema nada.
        assertTrue(IncidentType.ADBLUE.appliesTo(Powertrain.COMBUSTION, null))
        assertFalse(IncidentType.ADBLUE.appliesTo(Powertrain.FUEL_CELL, null))
    }

    private val now = 1_800_000_000_000L
    private fun ago(duration: kotlin.time.Duration) = now - duration.inWholeMilliseconds

    @Test
    fun `las abiertas van primero las más recientes`() {
        val view = incidentsView(
            listOf(
                Incident(id = "vieja", type = "custom", createdAt = ago(3.days)),
                Incident(id = "nueva", type = "custom", createdAt = ago(1.hours))
            ),
            now
        )
        assertEquals(listOf("nueva", "vieja"), view.open.map { it.id })
        assertTrue(view.resolved.isEmpty())
    }

    @Test
    fun `las resueltas se ven 30 días y después no`() {
        fun resolved(id: String, at: Long) =
            Incident(id = id, type = "custom", status = Incident.STATUS_RESOLVED, resolvedAt = at)
        val view = incidentsView(
            listOf(
                resolved("hace29", ago(29.days)),
                resolved("hace31", ago(31.days)),
                resolved("ayer", ago(1.days))
            ),
            now
        )
        assertEquals(listOf("ayer", "hace29"), view.resolved.map { it.id })
        assertTrue(view.open.isEmpty())
    }
}
