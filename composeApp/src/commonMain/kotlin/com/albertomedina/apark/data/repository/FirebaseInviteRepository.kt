package com.albertomedina.apark.data.repository

import com.albertomedina.apark.domain.model.JoinResult
import com.albertomedina.apark.domain.model.JoinStatus
import com.albertomedina.apark.domain.model.VehicleInvite
import com.albertomedina.apark.domain.repository.InviteRepository
import dev.gitlive.firebase.functions.FirebaseFunctions
import kotlinx.serialization.Serializable

class FirebaseInviteRepository(
    private val functions: FirebaseFunctions
) : InviteRepository {

    override suspend fun createInvite(vehicleId: String): Result<VehicleInvite> {
        return try {
            val response = functions
                .httpsCallable("createVehicleInvite")
                .invoke(CreateInviteRequest(vehicleId))
                .data<CreateInviteResponse>()

            Result.success(VehicleInvite(response.code, response.expiresAt))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun joinWithCode(code: String): Result<JoinResult> {
        return try {
            val response = functions
                .httpsCallable("joinVehicleWithCode")
                .invoke(JoinRequest(code))
                .data<JoinResponse>()

            Result.success(JoinResult(JoinStatus.from(response.status), response.vehicleName))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Serializable
    private data class CreateInviteRequest(val vehicleId: String)

    @Serializable
    private data class CreateInviteResponse(val code: String, val expiresAt: Long)

    @Serializable
    private data class JoinRequest(val code: String)

    @Serializable
    private data class JoinResponse(val status: String, val vehicleName: String = "")
}
