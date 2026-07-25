package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.VehicleRepository

class CreateVehicleUseCase(
    private val repository: VehicleRepository
) {
    suspend operator fun invoke(userId: String, name: String, licensePlate: String = ""): Result<Unit> {
        return repository.createVehicle(userId, name, licensePlate)
    }
}
