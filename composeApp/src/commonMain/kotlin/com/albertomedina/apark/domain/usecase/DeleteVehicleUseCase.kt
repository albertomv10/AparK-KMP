package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.VehicleRepository

class DeleteVehicleUseCase(
    private val repository: VehicleRepository
) {
    suspend operator fun invoke(vehicleId: String, userId: String): Result<Unit> {
        return repository.deleteVehicle(vehicleId, userId)
    }
}
