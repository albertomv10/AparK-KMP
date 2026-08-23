package com.albertomedina.apark.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val id: String = "",
    val name: String = "",
    val model: String = "",
    val licensePlate: String = "",
    val color: String = "",
    val ownerId: String = "",
    /**
     * Todos los miembros, **el dueño incluido**.
     *
     * Es lo que hace la pertenencia consultable: `array-contains` sobre este campo es una consulta
     * que Firestore puede demostrar segura solo con sus filtros, así que nunca devuelve un vehículo
     * que no puedas leer. Derivar la lista de un array de ids guardado en otro documento no daba esa
     * garantía — ver [spec 008](../../../../../../../docs/specs/008-vehicle-membership-model/spec.md).
     */
    val memberIds: List<String> = emptyList(),
    val lastLocation: LocationModel? = LocationModel(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)  {
    @Serializable
    data class LocationModel(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val timestamp: Long = 0L,
        val user: ParkedBy? = null
    )

    /**
     * Quién aparcó, con **lo que la tarjeta pinta y nada más**.
     *
     * Antes se guardaba aquí el objeto `User` entero, `userVehicles` incluido, de modo que cualquier
     * miembro de un vehículo compartido podía leer los ids de los demás vehículos de quien lo
     * aparcó. Una copia desnormalizada guarda lo que se muestra, no el objeto de origen.
     */
    @Serializable
    data class ParkedBy(
        val uid: String = "",
        val name: String = "",
        val email: String = ""
    )
}
