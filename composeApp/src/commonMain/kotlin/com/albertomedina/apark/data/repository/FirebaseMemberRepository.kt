package com.albertomedina.apark.data.repository

import com.albertomedina.apark.domain.model.NotAVehicleMemberException
import com.albertomedina.apark.domain.model.VehicleMember
import com.albertomedina.apark.domain.repository.MemberRepository
import dev.gitlive.firebase.functions.FirebaseFunctions
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException

class FirebaseMemberRepository(
    private val functions: FirebaseFunctions
) : MemberRepository {

    /**
     * No ser miembro vuelve como estado y no como error, igual que en `joinVehicleWithCode`: gitlive
     * no deja leer el código de un `HttpsError` desde aquí.
     */
    override suspend fun getMembers(vehicleId: String): Result<List<VehicleMember>> = try {
        val response = functions.httpsCallable("getVehicleMembers")
            .invoke(GetMembersRequest(vehicleId))
            .data<GetMembersResponse>()
        when (response.status) {
            "ok" -> Result.success(response.members.map { VehicleMember(it.uid, it.displayName, it.isOwner) })
            "not-member" -> Result.failure(NotAVehicleMemberException())
            else -> Result.failure(IllegalStateException("getVehicleMembers: status=${response.status}"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    @Serializable
    private data class GetMembersRequest(val vehicleId: String)

    @Serializable
    private data class GetMembersResponse(val status: String, val members: List<MemberDto> = emptyList())

    @Serializable
    private data class MemberDto(val uid: String, val displayName: String = "", val isOwner: Boolean = false)
}
