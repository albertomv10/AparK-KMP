package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.model.IncidentType
import com.albertomedina.apark.domain.repository.IncidentRepository

/**
 * Reportar una incidencia (spec 019). Se queda en lo que comprueban las reglas:
 * - una personalizada lleva su nombre, sin espacios en los bordes y sin pasar del límite. Si se
 *   queda vacío no se escribe nada, porque las reglas lo rechazarían;
 * - los demás tipos no llevan nombre: lo traduce la app;
 * - la nota va sin espacios en los bordes y sin pasar del límite.
 */
class ReportIncidentUseCase(private val repository: IncidentRepository) {
    suspend operator fun invoke(
        vehicleId: String,
        reportedBy: String,
        type: IncidentType,
        title: String,
        note: String
    ): Result<Unit> {
        val cleanTitle = if (type == IncidentType.CUSTOM) title.trim().take(Incident.TITLE_MAX_LENGTH) else ""
        if (type == IncidentType.CUSTOM && cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("A custom incident needs a name"))
        }
        return repository.report(
            vehicleId = vehicleId,
            reportedBy = reportedBy,
            type = type.code,
            title = cleanTitle,
            note = note.trim().take(Incident.NOTE_MAX_LENGTH)
        )
    }
}
