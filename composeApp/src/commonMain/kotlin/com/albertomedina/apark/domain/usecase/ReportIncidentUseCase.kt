package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.model.IncidentCategory
import com.albertomedina.apark.domain.model.IncidentTag
import com.albertomedina.apark.domain.repository.IncidentRepository

/**
 * Reportar una incidencia (spec 019). La etiqueta solo se guarda si es de la categoría, y la nota va
 * sin espacios en los bordes y sin pasar del límite: lo mismo que comprueban las reglas.
 */
class ReportIncidentUseCase(private val repository: IncidentRepository) {
    suspend operator fun invoke(
        vehicleId: String,
        reportedBy: String,
        category: IncidentCategory,
        tag: IncidentTag?,
        note: String
    ): Result<Unit> = repository.report(
        vehicleId = vehicleId,
        reportedBy = reportedBy,
        category = category.code,
        tag = tag?.takeIf { it.category == category }?.code ?: "",
        note = note.trim().take(Incident.NOTE_MAX_LENGTH)
    )
}
