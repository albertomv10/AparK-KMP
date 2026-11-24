package com.albertomedina.apark.domain.model

/**
 * Un miembro de un vehículo, tal como lo ven los demás (spec 014).
 *
 * [displayName] es su nombre o, si no tiene, su email: lo decide el servidor y solo manda uno de los
 * dos. Vacío cuando no hay ninguno, y entonces la pantalla pone un marcador.
 */
data class VehicleMember(
    val uid: String,
    val displayName: String,
    val isOwner: Boolean
)

/** Quien pide los miembros ya no lo es: le han quitado, o el vehículo se ha borrado. */
class NotAVehicleMemberException : Exception("Not a member of this vehicle")
