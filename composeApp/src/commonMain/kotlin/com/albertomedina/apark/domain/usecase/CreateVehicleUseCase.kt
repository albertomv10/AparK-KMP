package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.VehicleDetails
import com.albertomedina.apark.domain.repository.VehicleRepository

class CreateVehicleUseCase(
    private val repository: VehicleRepository
) {
    suspend operator fun invoke(userId: String, details: VehicleDetails): Result<Unit> =
        repository.createVehicle(userId, details)
}
