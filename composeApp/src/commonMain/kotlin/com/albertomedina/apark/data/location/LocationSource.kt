package com.albertomedina.apark.data.location

import com.albertomedina.apark.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface LocationSource {
    // Solo pide una ubicación fresca al hardware
    suspend fun getFreshLocation(): Vehicle.LocationModel?

    // Solo pide la última conocida
    suspend fun getLastKnownLocation(): Vehicle.LocationModel?
}