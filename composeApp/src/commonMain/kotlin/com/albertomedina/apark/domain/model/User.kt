package com.albertomedina.apark.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    /**
     * **Solo una pista de ordenación**, no la fuente de la lista: los vehículos salen de una
     * consulta sobre `memberIds`, y un id que sobre aquí se ignora sin más. Que dejara de ser la
     * fuente de la verdad es el cambio de fondo de la spec 008.
     */
    val userVehicles: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)