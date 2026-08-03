package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.UserRepository

class MoveVehicleUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: String, vehicleId: String, offset: Int): Result<Unit> {
        return repository.moveUserVehicle(userId, vehicleId, offset)
    }
}
