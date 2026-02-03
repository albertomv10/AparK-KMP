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
    private val locationSource: LocationSource, // Inyectamos la interfaz
    private val firestore: FirebaseFirestore //TODO quitar
) : LocationRepository {

    override fun getUserLocation(): Flow<Vehicle.LocationModel?> = flow {
        // AQUÍ está la lógica compartida: El timeout de 8 segundos
        val location = withTimeoutOrNull(8000L) {
            withContext(Dispatchers.Main){
                locationSource.getFreshLocation()
            }

        }

        if (location != null) {
            emit(location)
        } else {
            // El fallback compartido
            emit(locationSource.getLastKnownLocation())
        }
    }

    override suspend fun saveParking(data: Any) {
        firestore.collection("parkings").add(data) //TODO quitar
    }
}