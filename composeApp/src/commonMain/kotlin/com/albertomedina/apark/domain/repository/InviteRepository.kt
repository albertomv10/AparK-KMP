package com.albertomedina.apark.domain.repository

import com.albertomedina.apark.domain.model.JoinResult
import com.albertomedina.apark.domain.model.VehicleInvite

/**
 * Sharing runs through Cloud Functions, not Firestore directly: rules stop a client from finding
 * a vehicle it is not a member of, and from adding itself to that vehicle's members.
 */
interface InviteRepository {
    /** Creates a single-use invitation for a vehicle the caller owns, revoking any earlier one. */
    suspend fun createInvite(vehicleId: String): Result<VehicleInvite>

    /** Attempts to join the vehicle behind [code]. Failure means the call itself failed. */
    suspend fun joinWithCode(code: String): Result<JoinResult>
}
