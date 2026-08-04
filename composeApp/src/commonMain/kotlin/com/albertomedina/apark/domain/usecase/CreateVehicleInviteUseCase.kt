package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.VehicleInvite
import com.albertomedina.apark.domain.repository.InviteRepository

class CreateVehicleInviteUseCase(
    private val repository: InviteRepository
) {
    suspend operator fun invoke(vehicleId: String): Result<VehicleInvite> {
        return repository.createInvite(vehicleId)
    }
}
