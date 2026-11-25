package com.albertomedina.apark.data.repository

import com.albertomedina.apark.data.location.LocationSource
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.LocationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class LocationRepositoryImpl(
    private val locationSource: LocationSource,
) : LocationRepository {

    override suspend fun getApproximateLocation(): Vehicle.LocationModel? = try {
        withContext(Dispatchers.Default) {
            locationSource.getLastKnownLocation()
                ?: withTimeoutOrNull(5000L) { locationSource.getFreshLocation() }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Sin permiso, o sin servicios de ubicación: el mapa no se mueve, y ya está.
        null
    }

    override suspend fun getCurrentLocation(): Vehicle.LocationModel {
        // Esta función es la que usa el UseCase para "Aparcar aquí"
        return withContext(Dispatchers.Default) {
            val freshLocation = withTimeoutOrNull(5000L) {
                locationSource.getFreshLocation()
            }
            
            // Fallback: Si no hay fresca, intentamos la última conocida. 
            // Si ninguna existe, lanzamos excepción.
            freshLocation 
                ?: locationSource.getLastKnownLocation() 
                ?: throw Exception("No se ha podido obtener la ubicación actual")
        }
    }

}
