package com.albertomedina.apark.domain.model

/**
 * An invitation code for a vehicle. Single use, and short-lived: [expiresAtMillis] is set by the
 * server so a tampered client cannot extend it.
 */
data class VehicleInvite(
    val code: String,
    val expiresAtMillis: Long
)
