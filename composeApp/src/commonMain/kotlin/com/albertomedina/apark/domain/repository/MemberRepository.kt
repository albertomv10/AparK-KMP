package com.albertomedina.apark.domain.repository

import com.albertomedina.apark.domain.model.VehicleMember

/**
 * Los miembros de un vehículo, con su nombre. Viene del servidor porque nadie puede leer el `users`
 * de otro (spec 014). No es en tiempo real: se pide al abrir el detalle y cuando cambia quién está.
 */
interface MemberRepository {
    /**
     * El dueño primero y el resto por orden de entrada. Falla con
     * [com.albertomedina.apark.domain.model.NotAVehicleMemberException] si quien pide ya no es miembro.
     */
    suspend fun getMembers(vehicleId: String): Result<List<VehicleMember>>
}
