package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.IncidentRepository

/** «Deshacer» después de resolver una incidencia: vuelve a estar abierta (spec 019). */
class ReopenIncidentUseCase(private val repository: IncidentRepository) {
    suspend operator fun invoke(vehicleId: String, incidentId: String): Result<Unit> =
        repository.reopen(vehicleId, incidentId)
}
