package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.VehicleRepository

class TransferVehicleOwnershipUseCase (
    private val vehicleRepository: VehicleRepository
){
    suspend operator fun invoke(vehicleId: String, newOwnerId: String) {
        vehicleRepository.transferVehicleOwnership(vehicleId, newOwnerId)
    }
}