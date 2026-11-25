package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.LocationRepository

/** Dónde está quien mira, para la cámara del mapa (spec 017). `null` si no se puede saber. */
class LocateUserUseCase(
    private val repository: LocationRepository
) {
    suspend operator fun invoke(): Vehicle.LocationModel? = repository.getApproximateLocation()
}
