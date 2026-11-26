package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.IncidentRepository

/** Borrar una incidencia, para corregir un error: quien la reportó o el dueño (spec 019). */
class DeleteIncidentUseCase(private val repository: IncidentRepository) {
    suspend operator fun invoke(vehicleId: String, incidentId: String): Result<Unit> =
        repository.delete(vehicleId, incidentId)
}
