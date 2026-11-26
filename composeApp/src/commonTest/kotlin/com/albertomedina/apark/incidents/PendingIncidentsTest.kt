package com.albertomedina.apark.incidents

import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.model.OpenIncidents
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.openCount
import com.albertomedina.apark.domain.model.pendingOf
import com.albertomedina.apark.presentation.incidents.cardIncidents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Spec 020: lo pendiente de todos los vehículos, la chapita y los iconos de la tarjeta. */
class PendingIncidentsTest {

    private val furgo = Vehicle(id = "furgo", name = "Furgoneta")
    private val ibiza = Vehicle(id = "ibiza", name = "Ibiza")

    private fun open(id: String, at: Long, type: String = "low_fuel") =
        Incident(id = id, type = type, createdAt = at)

    private fun loaded(vararg incidents: Incident) = OpenIncidents.Loaded(incidents.sortedByDescending { it.createdAt })

    @Test
    fun `lo pendiente de todos va junto y la más reciente primero`() {
        val pending = pendingOf(
            listOf(furgo, ibiza),
            mapOf(
                "furgo" to loaded(open("f1", at = 100), open("f2", at = 300)),
                "ibiza" to loaded(open("i1", at = 200))
            )
        )
        assertEquals(listOf("f2", "i1", "f1"), pending.incidents.map { it.incident.id })
        assertEquals(listOf("Furgoneta", "Ibiza", "Furgoneta"), pending.incidents.map { it.vehicleName })
        assertTrue(pending.complete)
        assertFalse(pending.failed)
    }

    @Test
    fun `lo de un vehículo que ya no está en la lista no cuenta`() {
        val pending = pendingOf(
            listOf(furgo),
            mapOf("furgo" to loaded(), "fuera" to loaded(open("x", at = 1)), "roto" to OpenIncidents.Failed)
        )
        assertTrue(pending.incidents.isEmpty())
        assertTrue(pending.complete)
        assertFalse(pending.failed)
    }

    @Test
    fun `sin respuesta de algún vehículo no se sabe si está todo en orden`() {
        val pending = pendingOf(listOf(furgo, ibiza), mapOf("furgo" to loaded()))
        assertFalse(pending.complete)
        assertFalse(pending.failed)
    }

    @Test
    fun `un vehículo que falla se dice y lo de los demás se sigue viendo`() {
        val pending = pendingOf(
            listOf(furgo, ibiza),
            mapOf("furgo" to loaded(open("f1", at = 1)), "ibiza" to OpenIncidents.Failed)
        )
        assertEquals(listOf("f1"), pending.incidents.map { it.incident.id })
        assertTrue(pending.failed)
    }

    @Test
    fun `la chapita cuenta las abiertas de los vehículos que se le dan`() {
        val open = mapOf(
            "furgo" to loaded(open("f1", at = 1), open("f2", at = 2)),
            "ibiza" to loaded(open("i1", at = 3)),
            "roto" to OpenIncidents.Failed,
            "fuera" to loaded(open("x", at = 4))
        )
        assertEquals(3, openCount(setOf("furgo", "ibiza", "roto"), open))
        assertEquals(0, openCount(emptySet(), open))
    }

    @Test
    fun `la tarjeta enseña un icono por tipo y los de las más recientes`() {
        val card = cardIncidents(
            listOf(
                open("limpieza-vieja", at = 1, type = "cleaning"),
                open("gasolina", at = 5, type = "low_fuel"),
                open("limpieza", at = 4, type = "cleaning"),
                open("golpe", at = 3, type = "damage"),
                open("ruido", at = 2, type = "strange_noise")
            )
        )
        assertEquals(listOf("gasolina", "limpieza", "golpe"), card.shown.map { it.id })
        assertEquals(1, card.more)
    }

    @Test
    fun `con tres tipos o menos no sobra ninguno`() {
        val card = cardIncidents(listOf(open("a", at = 1, type = "cleaning"), open("b", at = 2, type = "cleaning")))
        assertEquals(listOf("b"), card.shown.map { it.id })
        assertEquals(0, card.more)
        assertEquals(0, cardIncidents(emptyList()).more)
    }

    @Test
    fun `las personalizadas y los tipos desconocidos comparten icono`() {
        val card = cardIncidents(listOf(open("silla", at = 2, type = "custom"), open("turbo", at = 1, type = "turbo")))
        assertEquals(listOf("silla"), card.shown.map { it.id })
    }
}
