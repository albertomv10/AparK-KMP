package com.albertomedina.apark.domain.model

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val userVehicles: List<String> = emptyList()
)