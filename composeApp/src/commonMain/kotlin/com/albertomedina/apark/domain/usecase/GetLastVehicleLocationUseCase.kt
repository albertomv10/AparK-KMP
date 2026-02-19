package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Vehicle.LocationModel
import com.albertomedina.apark.domain.repository.VehicleRepository

class GetLastVehicleLocationUseCase(
    private val repository: VehicleRepository
) {
    suspend operator fun invoke(vehicleId: String): LocationModel? {
        return repository.getLastVehicleLocation(vehicleId)
    }
}