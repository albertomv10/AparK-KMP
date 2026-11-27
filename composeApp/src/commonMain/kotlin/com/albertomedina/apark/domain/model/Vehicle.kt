package com.albertomedina.apark.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Vehicle(
    val id: String = "",
    val name: String = "",
    val licensePlate: String = "",
    // Los datos de la spec 015, como códigos o texto: ver `VehicleAttributes` y `VehicleDetails`.
    // `""` es «sin dato», y es también lo que vale un campo que el documento no tiene.
    val type: String = "",
    val powertrain: String = "",
    val fuel: String = "",
    val make: String = "",
    val model: String = "",
    val color: String = "",
    val emissionsLabel: String = "",
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
    /** Su ubicación predeterminada —el garaje—, o `null` si no tiene (spec 024). */
    val defaultPlace: DefaultPlace? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)  {
    @Serializable
    data class LocationModel(
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val timestamp: Long = 0L,
        val user: ParkedBy? = null,
        /**
         * Se aparcó con el botón de su ubicación predeterminada (spec 024). Aparcar con el GPS,
         * arrastrar el marcador o aparcar desde una versión anterior reescriben la ubicación entera, y
         * con ella se va la marca.
         */
        val atDefaultPlace: Boolean = false
    )

    /**
     * La ubicación predeterminada de un vehículo (spec 024): una sola, con su nombre. La pone,
     * cambia o quita cualquier miembro, y las reglas solo le dejan tocar esto.
     */
    @Serializable
    data class DefaultPlace(
        val name: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0
    ) {
        companion object {
            const val NAME_MAX_LENGTH = 40
        }
    }

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

/**
 * La ubicación predeterminada en la que está aparcado, o `null` si no está en ella (spec 024). Se
 * mira la marca de la última ubicación **y** que la ubicación siga existiendo: si alguien la quita con
 * el coche dentro, ya no hay sitio que nombrar.
 */
val Vehicle.parkedAtPlace: Vehicle.DefaultPlace?
    get() = defaultPlace?.takeIf { lastLocation?.atDefaultPlace == true }
