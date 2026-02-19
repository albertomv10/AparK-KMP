package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.VehicleRepository

class RemoveUserFromVehicleUseCase(
    private val repository: VehicleRepository
) {
    suspend operator fun invoke(vehicleId: String, userId: String) =
        repository.removeUserFromVehicle(vehicleId, userId)
}