package com.albertomedina.apark.incidents

import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.model.OpenIncidents
import com.albertomedina.apark.domain.repository.IncidentRepository
import com.albertomedina.apark.domain.usecase.ObserveOpenIncidentsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 020: las abiertas de varios vehículos a la vez, y un vehículo que falla no corta los demás. */
class ObserveOpenIncidentsTest {

    private val denied = IllegalStateException("PERMISSION_DENIED")

    /** Cada vehículo, con la escucha que le toca: unas entregan su lista, otras fallan. */
    private class FakeIncidents(private val listeners: Map<String, Flow<List<Incident>>>) : IncidentRepository {
        override fun observe(vehicleId: String) = listeners.getValue(vehicleId)
        override suspend fun report(vehicleId: String, reportedBy: String, type: String, title: String, note: String) = Result.success(Unit)
        override suspend fun resolve(vehicleId: String, incidentId: String, resolvedBy: String, expiresAtMillis: Long) = Result.success(Unit)
        override suspend fun reopen(vehicleId: String, incidentId: String) = Result.success(Unit)
        override suspend fun delete(vehicleId: String, incidentId: String) = Result.success(Unit)
    }

    private val gasolina = Incident(id = "gasolina", type = "low_fuel", createdAt = 1)
    private val golpe = Incident(id = "golpe", type = "damage", createdAt = 2)
    private val resuelta = Incident(id = "resuelta", type = "cleaning", status = Incident.STATUS_RESOLVED, createdAt = 3)

    private val repository = FakeIncidents(
        mapOf(
            "furgo" to flowOf(listOf(gasolina, resuelta, golpe)),
            "rota" to flow { throw denied }
        )
    )
    private val observe = ObserveOpenIncidentsUseCase(repository)

    @Test
    fun `solo llegan las abiertas y la más reciente primero`() = runTest {
        val last = observe(setOf("furgo"), isSignedIn = { true }) {}.toList().last()
        assertEquals(mapOf("furgo" to OpenIncidents.Loaded(listOf(golpe, gasolina))), last)
    }

    @Test
    fun `con la sesión viva un vehículo que falla se marca y los demás siguen`() = runTest {
        val failures = mutableListOf<Throwable>()
        val last = observe(setOf("furgo", "rota"), isSignedIn = { true }) { failures += it }.toList().last()
        assertEquals(OpenIncidents.Loaded(listOf(golpe, gasolina)), last["furgo"])
        assertEquals(OpenIncidents.Failed, last["rota"])
        assertEquals(listOf<Throwable>(denied), failures)
    }

    @Test
    fun `si la sesión terminó no es un fallo ni se marca`() = runTest {
        val failures = mutableListOf<Throwable>()
        val emitted = observe(setOf("furgo", "rota"), isSignedIn = { false }) { failures += it }.toList()
        assertTrue(emitted.none { OpenIncidents.Failed in it.values })
        assertTrue(failures.isEmpty())
    }

    @Test
    fun `sin vehículos no hay nada que escuchar`() = runTest {
        assertEquals(listOf(emptyMap()), observe(emptySet(), isSignedIn = { true }) {}.toList())
    }
}
