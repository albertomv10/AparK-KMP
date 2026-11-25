import CoreLocation
import GoogleMaps
import ComposeApp

class MapLocationController: NSObject, CLLocationManagerDelegate, GMSMapViewDelegate {
    
    static let shared = MapLocationController()
    
    let locationManager = CLLocationManager()
    var mapView: GMSMapView?

    /// Una orden de cámara que llegó antes de que existiera el mapa (spec 017). La fábrica la usa como
    /// cámara inicial, así que el mapa nace ya en su sitio en vez de en Madrid. Pasa al arrancar y al
    /// volver a la pestaña: `UIKitView` crea un `GMSMapView` nuevo cada vez.
    var pendingCamera: GMSCameraPosition?
    
    override init() {
        super.init()
        locationManager.delegate = self
    }
    
    func requestPermissionsAndStart() {
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
    }

    // Dónde estás para la cámara lo averigua Kotlin (`LocateUserUseCase`), igual que en Android: aquí
    // solo se pide el permiso al arrancar, para el punto azul del mapa.
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        manager.stopUpdatingLocation()
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("AparK: Error obteniendo ubicación (\(error.localizedDescription))")
    }
    
    func mapView(_ mapView: GMSMapView, didEndDragging marker: GMSMarker) {
            
            // 1. Rescatamos el ID del vehículo que guardamos en userData
            guard let vehicleId = marker.userData as? String else { return }
            
            let newLat = marker.position.latitude
            let newLng = marker.position.longitude
            
            // 2. Lo enviamos de vuelta a Kotlin
            AparKMap_iosKt.iosOnMarkerDragged?(
                vehicleId,
                KotlinDouble(value: newLat),
                KotlinDouble(value: newLng)
            )
        }

    /// Tocar un marcador elige su vehículo (spec 017). Devolver `true` evita lo de por defecto —centrarlo
    /// y abrir el globo—: la cámara ya va a él por la regla común.
    func mapView(_ mapView: GMSMapView, didTap marker: GMSMarker) -> Bool {
        guard let vehicleId = marker.userData as? String else { return false }
        AparKMap_iosKt.iosOnMarkerTapped?(vehicleId)
        return true
    }

    /// Mantener pulsado el mapa vacío mueve ahí el vehículo elegido (spec 017). Sobre un marcador, el
    /// mismo gesto empieza a arrastrarlo, así que no chocan.
    func mapView(_ mapView: GMSMapView, didLongPressAt coordinate: CLLocationCoordinate2D) {
        AparKMap_iosKt.iosOnMapLongPressed?(
            KotlinDouble(value: coordinate.latitude),
            KotlinDouble(value: coordinate.longitude)
        )
    }
}
