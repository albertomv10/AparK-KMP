package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.VehicleRepository

class GetVehicleByIdUseCase(
    private val repository: VehicleRepository
) {
    suspend operator fun invoke(vehicleId: String) = repository.getVehicleById(vehicleId)
}