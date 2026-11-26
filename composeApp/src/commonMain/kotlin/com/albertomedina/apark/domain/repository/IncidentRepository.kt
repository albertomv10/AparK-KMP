package com.albertomedina.apark.domain.repository

import com.albertomedina.apark.domain.model.Incident
import kotlinx.coroutines.flow.Flow

/**
 * Las incidencias de un vehículo (spec 019), en `vehicles/{id}/incidents`.
 *
 * **Las escrituras no terminan hasta que el servidor confirma**, y sin conexión no terminan nunca,
 * aunque el caché ya las enseñe: quien las llama no debe esperarlas para seguir.
 */
interface IncidentRepository {
    /** Todas las del vehículo, en tiempo real. Si se pierde el acceso, una lista vacía. */
    fun observe(vehicleId: String): Flow<List<Incident>>

    /** [title] es el nombre de una personalizada, y `""` en los demás tipos: lo exigen las reglas. */
    suspend fun report(vehicleId: String, reportedBy: String, type: String, title: String, note: String): Result<Unit>

    suspend fun resolve(vehicleId: String, incidentId: String, resolvedBy: String, expiresAtMillis: Long): Result<Unit>

    /** «Deshacer» una resolución: vuelve a abierta, sin quién ni cuándo ni caducidad. */
    suspend fun reopen(vehicleId: String, incidentId: String): Result<Unit>

    suspend fun delete(vehicleId: String, incidentId: String): Result<Unit>
}
