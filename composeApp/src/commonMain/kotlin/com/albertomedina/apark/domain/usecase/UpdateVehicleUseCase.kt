package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.VehicleDetails
import com.albertomedina.apark.domain.repository.VehicleRepository

/** Editar los datos de un vehículo (spec 015). Solo el dueño: las reglas se lo deniegan al resto. */
class UpdateVehicleUseCase(
    private val repository: VehicleRepository
) {
    suspend operator fun invoke(vehicleId: String, details: VehicleDetails): Result<Unit> =
        repository.updateVehicle(vehicleId, details)
}
