package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.repository.UserRepository

/** Guarda el orden de los vehículos de una persona, el mismo en el carrusel y en la lista (spec 016). */
class SetVehicleOrderUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: String, vehicleIds: List<String>): Result<Unit> =
        repository.setVehicleOrder(userId, vehicleIds)
}
