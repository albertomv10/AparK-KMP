package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.VehicleRepository

/**
 * Pasa la propiedad a otro miembro (spec 014). Quien la pasa sigue siendo miembro. Las reglas solo
 * dejan dársela a alguien que ya está en `memberIds`.
 */
class TransferVehicleOwnershipUseCase(
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke(vehicleId: String, newOwnerId: String): Result<Unit> =
        vehicleRepository.transferVehicleOwnership(vehicleId, newOwnerId)
}
