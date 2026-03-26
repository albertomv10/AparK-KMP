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

class AndroidLocationSource(
    private val context: Context
) : LocationSource {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    override suspend fun getFreshLocation(): Vehicle.LocationModel? = suspendCancellableCoroutine { continuation ->
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        ).build()

        var bestLocation: Location? = null

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                
                // Guardamos la mejor hasta ahora por si hay timeout
                if (bestLocation == null || location.accuracy < (bestLocation?.accuracy ?: Float.MAX_VALUE)) {
                    bestLocation = location
                }

                // Si llegamos a 20m (igual que iOS), devolvemos ya
                if (location.accuracy <= 20f) {
                    if (continuation.isActive) {
                        continuation.resume(location.toDomainModel())
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        continuation.invokeOnCancellation {
            // Si el repositorio cancela por timeout, intentamos devolver la mejor que pillamos
            if (!continuation.isCompleted) {
                // Nota: en un suspendCancellableCoroutine real no puedes resumir aquí fácilmente,
                // pero al menos nos aseguramos de parar el GPS.
            }
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

    private fun Location.toDomainModel(): Vehicle.LocationModel {
        return Vehicle.LocationModel(
            latitude = this.latitude,
            longitude = this.longitude,
            timestamp = this.time,
            user = null
        )
    }
}
