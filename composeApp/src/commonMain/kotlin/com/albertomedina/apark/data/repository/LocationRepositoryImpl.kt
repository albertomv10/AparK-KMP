package com.albertomedina.apark.data.repository

import com.albertomedina.apark.data.location.LocationSource
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.LocationRepository
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class LocationRepositoryImpl(
    private val locationSource: LocationSource,
    private val firestore: FirebaseFirestore //TODO quitar
) : LocationRepository {

    override fun getUserLocation(): Flow<Vehicle.LocationModel?> = flow {
        // Intentamos obtener una ubicación fresca con un timeout
        val location = withTimeoutOrNull(8000L) {
            locationSource.getFreshLocation()
        }

        if (location != null) {
            emit(location)
        } else {
            // Si falla el timeout, emitimos la última conocida
            emit(locationSource.getLastKnownLocation())
        }
    }

    override suspend fun saveParking(data: Any) {
        firestore.collection("parkings").add(data) //TODO quitar
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

    override fun getLocationUpdates(): Flow<Vehicle.LocationModel> {
        // Podrías implementar esto si quieres seguimiento en tiempo real
        TODO("Not yet implemented")
    }
}
