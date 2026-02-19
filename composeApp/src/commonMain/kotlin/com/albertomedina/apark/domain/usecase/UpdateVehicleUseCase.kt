package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.VehicleRepository

class UpdateVehicleUseCase(
    private val repository: VehicleRepository
) {
    suspend operator fun invoke(vehicle: Vehicle): Result<Unit> {
        return repository.updateVehicle(vehicle)
    }
}