package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.VehicleRepository

/**
 * El dueño quita a otro miembro (spec 014). Salirse uno mismo es [RemoveUserFromVehicleUseCase], que
 * además limpia tu propia lista; aquí no se puede, porque las reglas no dejan escribir el `users` de
 * otro.
 */
class RemoveVehicleMemberUseCase(
    private val vehicleRepository: VehicleRepository
) {
    suspend operator fun invoke(vehicleId: String, memberId: String): Result<Unit> =
        vehicleRepository.removeMember(vehicleId, memberId)
}
