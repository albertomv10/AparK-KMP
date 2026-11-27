package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.VehicleRepository

/**
 * Pone o cambia la ubicación predeterminada de un vehículo (spec 024).
 *
 * El nombre se guarda sin los espacios de los bordes y cortado a 40, que es lo que admiten las
 * reglas. Si queda vacío, se usa [defaultName], «Garaje» en el idioma de quien la pone: es lo que el
 * campo ya proponía.
 */
class SetDefaultPlaceUseCase(private val repository: VehicleRepository) {
    suspend operator fun invoke(
        vehicleId: String,
        name: String,
        latitude: Double,
        longitude: Double,
        defaultName: String
    ): Result<Unit> = repository.setDefaultPlace(
        vehicleId,
        Vehicle.DefaultPlace(
            name = placeName(name, defaultName),
            latitude = latitude,
            longitude = longitude
        )
    )

    companion object {
        fun placeName(name: String, defaultName: String): String =
            name.trim().ifEmpty { defaultName.trim() }.take(Vehicle.DefaultPlace.NAME_MAX_LENGTH).trim()
    }
}
