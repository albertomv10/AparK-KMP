package com.albertomedina.apark.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val id: String = "",
    val name: String = "",
    val model: String = "",
    val licensePlate: String = "",
    val color: String = "",
    val inviteCode: String? = null,
    val ownerId: String = "",
    val sharedUsers: List<String> = emptyList(),
    val lastLocation: LocationModel? = LocationModel()
)  {
    @Serializable
    data class LocationModel(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val timestamp: Long = 0L,
        val user: User? = null
    )
}