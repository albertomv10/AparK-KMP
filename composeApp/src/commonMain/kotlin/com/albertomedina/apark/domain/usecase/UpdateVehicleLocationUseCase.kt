package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.LocationRepository
import com.albertomedina.apark.domain.repository.VehicleRepository

class UpdateVehicleLocationUseCase(
    private val vehicleRepository: VehicleRepository,
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(vehicleId: String, previousLocation:Vehicle.LocationModel? = null): Result<Unit> {
        return try {
            val location = previousLocation ?: locationRepository.getCurrentLocation()

            vehicleRepository.updateVehicleLocation(vehicleId, location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
