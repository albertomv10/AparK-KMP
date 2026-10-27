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
                    // Solo lo que la tarjeta pinta. Guardar el `User` entero metía su `userVehicles`
                    // dentro del vehículo, y con él los ids de los demás coches de quien aparcó.
                    user = Vehicle.ParkedBy(
                        uid = userId,
                        name = userProfile.name,
                        // El del token, ni el del documento ni el del perfil: es el que la regla de
                        // aparcar compara. El del documento se queda atrás cuando el email cambia
                        // fuera de la app, y con él un miembro que no es dueño no podía volver a
                        // aparcar (spec 011). El del perfil se adelanta al token tras un `reload`.
                        email = authRepository.tokenEmail()
                            ?: authRepository.getUserEmail()
                            ?: userProfile.email
                    ),
                    timestamp = if (location.timestamp == 0L) Clock.System.now().toEpochMilliseconds() else location.timestamp
                )
            }

            vehicleRepository.updateVehicleLocation(vehicleId, finalLocation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
