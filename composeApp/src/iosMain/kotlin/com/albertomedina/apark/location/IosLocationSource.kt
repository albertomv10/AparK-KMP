package com.albertomedina.apark.data.location

import com.albertomedina.apark.domain.model.Vehicle
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreLocation.kCLLocationAccuracyNearestTenMeters
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Implementación nativa para iOS usando CoreLocation
class IosLocationSource : LocationSource {

    // El Manager de iOS. Lo instanciamos aquí.
    private val locationManager = CLLocationManager()

    override suspend fun getFreshLocation(): Vehicle.LocationModel? = suspendCancellableCoroutine { continuation ->

        // 1. Configuramos el Manager (Equivalente al LocationRequest de Android)
        locationManager.desiredAccuracy = kCLLocationAccuracyBest // Alta precisión
        locationManager.distanceFilter = 10.0 // Metros mínimos para notificar (opcional)

        // 2. Creamos el Delegado (Quien recibe los eventos de iOS)
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {

            // Éxito: iOS nos manda nuevas ubicaciones
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return

                // 3. Filtro de precisión (Igual que en tu Android)
                // horizontalAccuracy en iOS es en metros.
                // Si es negativo, la ubicación es inválida.
                if (location.horizontalAccuracy >= 0 && location.horizontalAccuracy < 20.0) {
                    if (continuation.isActive) {
                        continuation.resume(location.toDomainModel())
                        manager.stopUpdatingLocation() // Ya tenemos lo que queríamos
                    }
                }
            }

            // Error: Algo falló (GPS apagado, sin permisos, etc)
            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                // A veces iOS lanza un error inmediato si no hay señal,
                // pero puede seguir intentándolo. Depende de tu lógica si quieres cancelar ya.
                // Aquí, por seguridad, si falla notificamos.
                if (continuation.isActive) {
                    continuation.resumeWithException(Exception(didFailWithError.localizedDescription))
                }
            }
        }

        // Asignamos el delegado
        locationManager.delegate = delegate

        // 4. Pedimos permisos (Importante en iOS solicitarlos si no se tienen)
        // Normalmente esto se pide en la UI, pero llamar a esto es seguro.
        locationManager.requestWhenInUseAuthorization()

        // 5. Arrancamos la búsqueda
        locationManager.startUpdatingLocation()

        // 6. Limpieza: Si la corrutina se cancela (timeout del repositorio), paramos el GPS.
        continuation.invokeOnCancellation {
            locationManager.stopUpdatingLocation()
            // Importante: Desvincular el delegado para evitar fugas de memoria o crashes
            locationManager.delegate = null
        }
    }

    override suspend fun getLastKnownLocation(): Vehicle.LocationModel? {
        // En iOS, 'location' contiene la última ubicación conocida
        val location = locationManager.location
        return location?.toDomainModel()
    }

    // --- Mapeo a tu Modelo de Dominio ---
    @OptIn(ExperimentalForeignApi::class)
    private fun CLLocation.toDomainModel(): Vehicle.LocationModel {
        return Vehicle.LocationModel(
            latitude = this.coordinate.useContents{latitude},
            longitude = this.coordinate.useContents { longitude },
            // iOS da el tiempo en segundos desde 2001 o 1970 dependiendo la propiedad.
            // timeIntervalSince1970 da segundos, multiplicamos por 1000 para milisegundos (standard Kotlin)
            timestamp = (this.timestamp.timeIntervalSince1970 * 1000).toLong(),
            user = ""
        )
    }
}