package com.albertomedina.apark.domain.repository

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.VehicleDetails
import com.albertomedina.apark.domain.model.Vehicle.LocationModel
import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    fun getVehiclesForUser(userId: String): Flow<List<Vehicle>>
    suspend fun getLastVehicleLocation(vehicleId: String): LocationModel?
    suspend fun updateVehicleLocation(vehicleId: String, location: LocationModel): Result<Unit>
    suspend fun createVehicle(userId: String, details: VehicleDetails): Result<Unit>
    suspend fun deleteVehicle(vehicleId: String, userId: String): Result<Unit>
    /** Solo los datos editables y `updatedAt`: nunca el dueño, los miembros ni la ubicación. */
    suspend fun updateVehicle(vehicleId: String, details: VehicleDetails): Result<Unit>
    /** Salirse: quita a [userId] del vehículo y el vehículo de su lista. Solo vale para uno mismo. */
    suspend fun removeUserFromVehicle(vehicleId: String, userId: String): Result<Unit>
    /** El dueño quita a otro. No toca el `users` de esa persona, que las reglas no dejan escribir. */
    suspend fun removeMember(vehicleId: String, memberId: String): Result<Unit>
    suspend fun transferVehicleOwnership(vehicleId: String, newOwnerId: String): Result<Unit>

    /**
     * Pone o cambia la ubicación predeterminada (spec 024). Como toda escritura, no termina hasta que
     * el servidor confirma, y sin conexión no termina nunca: quien la llama no debe esperarla.
     */
    suspend fun setDefaultPlace(vehicleId: String, place: Vehicle.DefaultPlace): Result<Unit>

    /** La quita: borra el campo. Igual que [setDefaultPlace], no se espera. */
    suspend fun removeDefaultPlace(vehicleId: String): Result<Unit>
}