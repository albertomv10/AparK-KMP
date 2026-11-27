package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.VehicleRepository

/** Quita la ubicación predeterminada de un vehículo (spec 024). */
class RemoveDefaultPlaceUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke(vehicleId: String): Result<Unit> = repository.removeDefaultPlace(vehicleId)
}
