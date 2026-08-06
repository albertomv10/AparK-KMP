package com.albertomedina.apark.domain.repository

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.Vehicle.LocationModel
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun getVehiclesForUser(userId: String): Flow<List<Vehicle>>
    suspend fun getVehicleById(vehicleId: String): Vehicle?
    suspend fun getLastVehicleLocation(vehicleId: String): LocationModel?
    suspend fun updateVehicleLocation(vehicleId: String, location: LocationModel): Result<Unit>
    suspend fun shareVehicleWithUser(vehicleId: String, userId: String)
    suspend fun createVehicle(userId: String, name: String, licensePlate: String = ""): Result<Unit>
    suspend fun deleteVehicle(vehicleId: String, userId: String): Result<Unit>
    suspend fun updateVehicle(vehicle: Vehicle): Result<Unit>
    suspend fun removeUserFromVehicle(vehicleId: String, userId: String): Result<Unit>
    suspend fun transferVehicleOwnership(vehicleId: String, newOwnerId: String): Result<Unit>
}