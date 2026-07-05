package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.VehicleRepository

class ShareVehicleWithUserUseCase (
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke(vehicleId: String, userId: String) {
        vehicleRepository.shareVehicleWithUser(vehicleId, userId)
    }
}