package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow

class GetVehicleListUseCase(
    private val repository: VehicleRepository
) {
    operator fun invoke(userId: String): Flow<List<Vehicle>> {
        return repository.getVehiclesForUser(userId)
    }
}