package com.albertomedina.apark.data.location

import com.google.android.gms.location.FusedLocationProviderClient
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.albertomedina.apark.domain.model.Vehicle
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Implementa la interfaz compartida LocationSource
class AndroidLocationSource(
    private val context: Context
) : LocationSource {

    // Inicializamos el cliente de ubicación de forma perezosa
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    override suspend fun getFreshLocation(): Vehicle.LocationModel? = suspendCancellableCoroutine { continuation ->

        // 1. Configuración exigente (Alta precisión)
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        ).apply {
            setMinUpdateIntervalMillis(500)
            setMaxUpdates(10) // Evita que se quede escuchando eternamente si algo falla
        }.build()

        // 2. Definimos el Callback
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                // 3. El filtro de precisión (< 15 metros)
                // Si la ubicación es buena, la devolvemos y cerramos el chiringuito.
                if (location.accuracy < 15f) {
                    if (continuation.isActive) {
                        continuation.resume(location.toDomainModel())
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            }
        }

        // 4. Solicitamos las actualizaciones
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // 5. Limpieza automática
        // Esto se ejecuta si el repositorio cancela la corrutina (el timeout de 8s)
        continuation.invokeOnCancellation {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): Vehicle.LocationModel? = suspendCancellableCoroutine { continuation ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (continuation.isActive) {
                    continuation.resume(location?.toDomainModel())
                }
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
    }

    // --- Función de Mapeo Privada ---
    // Convierte el objeto nativo de Android al modelo compartido de tu Dominio
    private fun Location.toDomainModel(): Vehicle.LocationModel {
        return Vehicle.LocationModel(
            latitude = this.latitude,
            longitude = this.longitude,
            timestamp = this.time,
            user = "" // El repositorio o el UseCase se encargarán de poner el ID del usuario si hace falta
        )
    }
}