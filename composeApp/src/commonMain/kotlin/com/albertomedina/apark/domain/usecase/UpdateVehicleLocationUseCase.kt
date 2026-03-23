package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.LocationRepository
import com.albertomedina.apark.domain.repository.VehicleRepository

class UpdateVehicleLocationUseCase(
    private val vehicleRepository: VehicleRepository,
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(vehicleId: String): Result<Unit> {
        return try {
            // 1. Obtenemos la ubicación dentro del caso de uso
            val location = locationRepository.getCurrentLocation()
            
            // 2. Actualizamos el repositorio de vehículos
            vehicleRepository.updateVehicleLocation(vehicleId, location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
