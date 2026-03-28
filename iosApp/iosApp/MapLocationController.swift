import CoreLocation
import GoogleMaps
import ComposeApp

class MapLocationController: NSObject, CLLocationManagerDelegate, GMSMapViewDelegate {
    
    static let shared = MapLocationController()
    
    let locationManager = CLLocationManager()
    var mapView: GMSMapView?
    
    private var shouldCenterCamera = false
    // 👇 Guardamos si la cámara debe animarse cuando el GPS responda
    private var animateNextCameraMove = false
    
    override init() {
        super.init()
        locationManager.delegate = self
    }
    
    func requestPermissionsAndStart() {
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
    }
    
    // Recibimos el parámetro animated
    func centerOnUserLocation(animated: Bool) {
        locationManager.requestWhenInUseAuthorization()
        
        if let location = mapView?.myLocation ?? locationManager.location {
            // Ya tenemos la ubicación instantáneamente
            moveCamera(to: location.coordinate, animated: animated)
        } else {
            // No la tenemos. Encendemos el GPS y guardamos si queríamos animación
            shouldCenterCamera = true
            animateNextCameraMove = animated
            locationManager.startUpdatingLocation()
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.first else { return }
        
        if shouldCenterCamera {
            // Movemos la cámara usando la preferencia que guardamos
            moveCamera(to: location.coordinate, animated: animateNextCameraMove)
            shouldCenterCamera = false
        }
        
        manager.stopUpdatingLocation()
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("AparK: Error obteniendo ubicación (\(error.localizedDescription))")
        shouldCenterCamera = false
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
    
    // Función auxiliar para no repetir el código del CATransaction
    private func moveCamera(to coordinate: CLLocationCoordinate2D, animated: Bool) {
        guard let map = mapView else { return }
        let camera = GMSCameraPosition.camera(withTarget: coordinate, zoom: 15)
        
        CATransaction.begin()
        if animated {
            CATransaction.setAnimationDuration(0.8)
            CATransaction.setAnimationTimingFunction(CAMediaTimingFunction(name: .easeInEaseOut))
        } else {
            // Teletransporte instantáneo (0.0 segundos)
            CATransaction.setAnimationDuration(0.0)
        }
        map.animate(to: camera)
        CATransaction.commit()
    }
}
