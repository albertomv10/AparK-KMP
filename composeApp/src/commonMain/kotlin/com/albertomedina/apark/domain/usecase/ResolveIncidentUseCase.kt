package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.repository.IncidentRepository
import kotlin.time.Clock

/**
 * Resolver una incidencia (spec 019). La caducidad es dentro de 30 días: entonces Firestore la borra
 * sola (TTL). La calcula el móvil, y las reglas solo impiden que pase de 31.
 */
class ResolveIncidentUseCase(private val repository: IncidentRepository) {
    suspend operator fun invoke(vehicleId: String, incidentId: String, resolvedBy: String): Result<Unit> {
        val expiresAt = Clock.System.now() + Incident.RESOLVED_RETENTION
        return repository.resolve(vehicleId, incidentId, resolvedBy, expiresAt.toEpochMilliseconds())
    }
}
