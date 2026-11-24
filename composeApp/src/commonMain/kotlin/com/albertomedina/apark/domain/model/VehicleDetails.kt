package com.albertomedina.apark.domain.model

/**
 * Lo que se puede editar de un vehículo (spec 015): ni el dueño ni los miembros ni la ubicación.
 *
 * Las listas van como **códigos en texto**, igual que en el documento, y no como sus `enum`: si esta
 * versión no conoce un código, editar el vehículo sin tocar ese campo lo conserva tal cual, en vez de
 * cambiarlo por «otro». Se traducen a sus tipos solo para mostrarlos o elegir.
 */
data class VehicleDetails(
    val name: String,
    val licensePlate: String = "",
    val type: String = "",
    val powertrain: String = "",
    val fuel: String = "",
    val make: String = "",
    val model: String = "",
    val color: String = "",
    val emissionsLabel: String = ""
)

fun Vehicle.details(): VehicleDetails = VehicleDetails(
    name = name,
    licensePlate = licensePlate,
    type = type,
    powertrain = powertrain,
    fuel = fuel,
    make = make,
    model = model,
    color = color,
    emissionsLabel = emissionsLabel
)

/** Si tiene alguno de los datos de la spec 015. Sin ninguno, el detalle invita al dueño a añadirlos. */
val Vehicle.hasDetails: Boolean
    get() = listOf(type, powertrain, fuel, make, model, color, emissionsLabel).any { it.isNotBlank() }
