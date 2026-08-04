package com.albertomedina.apark.data.repository

import com.albertomedina.apark.di.AppConfig
import com.albertomedina.apark.domain.model.JoinResult
import com.albertomedina.apark.domain.model.JoinStatus
import com.albertomedina.apark.domain.model.VehicleInvite
import com.albertomedina.apark.domain.repository.InviteRepository
import dev.gitlive.firebase.functions.FirebaseFunctions
import kotlinx.serialization.Serializable

class FirebaseInviteRepository(
    private val functions: FirebaseFunctions,
    private val config: AppConfig
) : InviteRepository {

    override suspend fun createInvite(vehicleId: String): Result<VehicleInvite> {
        return try {
            val response = functions
                .httpsCallable(named("createVehicleInvite"))
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
                .httpsCallable(named("joinVehicleWithCode"))
                .invoke(JoinRequest(code))
                .data<JoinResponse>()

            Result.success(JoinResult(JoinStatus.from(response.status), response.vehicleName))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * A callable cannot tell which Firestore database the caller uses, the way a trigger can, so
     * each one is deployed twice and the build picks its own.
     */
    private fun named(base: String): String = if (config.isDebug) "${base}Debug" else base

    @Serializable
    private data class CreateInviteRequest(val vehicleId: String)

    @Serializable
    private data class CreateInviteResponse(val code: String, val expiresAt: Long)

    @Serializable
    private data class JoinRequest(val code: String)

    @Serializable
    private data class JoinResponse(val status: String, val vehicleName: String = "")
}
