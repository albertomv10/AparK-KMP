package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.repository.LocationRepository
import com.albertomedina.apark.domain.repository.UserRepository
import com.albertomedina.apark.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

class UpdateVehicleLocationUseCase(
    private val vehicleRepository: VehicleRepository,
    private val locationRepository: LocationRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        vehicleId: String,
        manualLocation:Vehicle.LocationModel? = null,
        isUndo: Boolean = false
    ): Result<Unit> {
        return try {
            val finalLocation = if (isUndo && manualLocation != null) {
                manualLocation
            } else {

                val location = manualLocation?: locationRepository.getCurrentLocation()

                val userId = authRepository.getCurrentUser()?.uid
                    ?: return Result.failure(Exception("No user logged in"))

                val userProfile = userRepository.getUser(userId).first()

                location.copy(
                    user = userProfile,
                    timestamp = if (location.timestamp == 0L) Clock.System.now().toEpochMilliseconds() else location.timestamp
                )
            }

            vehicleRepository.updateVehicleLocation(vehicleId, finalLocation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
