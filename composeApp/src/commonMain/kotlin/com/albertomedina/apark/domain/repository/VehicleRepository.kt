package com.albertomedina.apark.domain.repository

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.Vehicle.LocationModel
import kotlinx.coroutines.flow.Flow

interface CarRepository {
    fun getCarsForUser(userId: String): Flow<List<Vehicle>>
    suspend fun getCarById(carId: String): Vehicle?
    suspend fun getLastCarLocation(carId: String): LocationModel?
    suspend fun updateCarLocation(carId: String, location: LocationModel): Result<Unit>
    suspend fun shareCarWithUser(carId: String, userId: String)
    suspend fun createVehicle(userId: String, name: String): Result<Unit>
    suspend fun updateVehicle(vehicle: Vehicle): Result<Unit>
    suspend fun joinCarByCodeOrId(identifier: String, userId: String): Result<Unit>
    suspend fun removeUserFromCar(carId: String, userId: String): Result<Unit>
    suspend fun transferCarOwnership(carId: String, newOwnerId: String): Result<Unit>
}