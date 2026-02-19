package com.albertomedina.apark.domain.repository

import com.albertomedina.apark.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getUserLocation(): Flow<Vehicle.LocationModel?>

    suspend fun saveParking(data: Any) //TODO quitar

    suspend fun getCurrentLocation(): Vehicle.LocationModel
    fun getLocationUpdates(): Flow<Vehicle.LocationModel>
}