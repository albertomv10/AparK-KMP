package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.Flow

class ObserveIncidentsUseCase(private val repository: IncidentRepository) {
    operator fun invoke(vehicleId: String): Flow<List<Incident>> = repository.observe(vehicleId)
}
