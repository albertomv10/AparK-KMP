package com.albertomedina.apark.incidents

import com.albertomedina.apark.domain.model.Fuel
import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.model.IncidentCategory
import com.albertomedina.apark.domain.model.IncidentTag
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
        IncidentCategory.entries.forEach { assertEquals(it, IncidentCategory.fromCode(it.code)) }
        IncidentTag.entries.forEach { assertEquals(it, IncidentTag.fromCode(it.code)) }
    }

    @Test
    fun `una categoría desconocida sale como otro y una etiqueta desconocida no sale`() {
        assertEquals(IncidentCategory.OTHER, IncidentCategory.fromCode("engine"))
        assertEquals(IncidentCategory.OTHER, IncidentCategory.fromCode(""))
        assertNull(IncidentTag.fromCode("flat_battery"))
    }

    @Test
    fun `cada etiqueta es de una categoría y otro no tiene ninguna`() {
        assertEquals(5, IncidentCategory.LEVELS.tags.size)
        assertEquals(5, IncidentCategory.MECHANICS.tags.size)
        assertEquals(3, IncidentCategory.TYRES.tags.size)
        assertEquals(3, IncidentCategory.BODY.tags.size)
        assertTrue(IncidentCategory.OTHER.tags.isEmpty())
    }

    @Test
    fun `una etiqueta que no es de su categoría no se enseña`() {
        assertNull(Incident(id = "i", category = "tyres", tag = "cleaning").tagOrNull)
        assertEquals(IncidentTag.PUNCTURE, Incident(id = "i", category = "tyres", tag = "puncture").tagOrNull)
    }

    @Test
    fun `sin datos del vehículo se ofrecen todas las etiquetas`() {
        assertTrue(IncidentTag.entries.all { it.appliesTo(null, null) })
    }

    @Test
    fun `un eléctrico se carga y no reposta ni lleva aceite`() {
        assertTrue(IncidentTag.LOW_CHARGE.appliesTo(Powertrain.ELECTRIC, null))
        assertFalse(IncidentTag.LOW_FUEL.appliesTo(Powertrain.ELECTRIC, null))
        assertFalse(IncidentTag.OIL.appliesTo(Powertrain.ELECTRIC, null))
        assertFalse(IncidentTag.ADBLUE.appliesTo(Powertrain.ELECTRIC, null))
    }

    @Test
    fun `solo se carga lo que se enchufa`() {
        assertTrue(IncidentTag.LOW_CHARGE.appliesTo(Powertrain.PLUG_IN_HYBRID, Fuel.PETROL))
        assertFalse(IncidentTag.LOW_CHARGE.appliesTo(Powertrain.HYBRID, Fuel.PETROL))
        assertFalse(IncidentTag.LOW_CHARGE.appliesTo(Powertrain.COMBUSTION, Fuel.DIESEL))
    }

    @Test
    fun `AdBlue solo en un diésel`() {
        assertTrue(IncidentTag.ADBLUE.appliesTo(Powertrain.COMBUSTION, Fuel.DIESEL))
        assertFalse(IncidentTag.ADBLUE.appliesTo(Powertrain.COMBUSTION, Fuel.PETROL))
        // Sin combustible indicado se ofrece, salvo en lo que no quema nada.
        assertTrue(IncidentTag.ADBLUE.appliesTo(Powertrain.COMBUSTION, null))
        assertFalse(IncidentTag.ADBLUE.appliesTo(Powertrain.FUEL_CELL, null))
    }

    private val now = 1_800_000_000_000L
    private fun ago(duration: kotlin.time.Duration) = now - duration.inWholeMilliseconds

    @Test
    fun `las abiertas van primero las más recientes`() {
        val view = incidentsView(
            listOf(
                Incident(id = "vieja", category = "other", createdAt = ago(3.days)),
                Incident(id = "nueva", category = "other", createdAt = ago(1.hours))
            ),
            now
        )
        assertEquals(listOf("nueva", "vieja"), view.open.map { it.id })
        assertTrue(view.resolved.isEmpty())
    }

    @Test
    fun `las resueltas se ven 30 días y después no`() {
        fun resolved(id: String, at: Long) =
            Incident(id = id, category = "other", status = Incident.STATUS_RESOLVED, resolvedAt = at)
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
