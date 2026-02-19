package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Vehicle.LocationModel
import com.albertomedina.apark.domain.repository.LocationRepository

class GetCurrentLocationUseCase(
    private val repository: LocationRepository
) {
    suspend operator fun invoke(): LocationModel {
        return repository.getCurrentLocation()
    }
}