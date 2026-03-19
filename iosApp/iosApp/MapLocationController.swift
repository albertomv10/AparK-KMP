//
//  MapLocationController.swift
//  iosApp
//
//  Created by Alberto Medina on 19/3/26.
//

import CoreLocation
import GoogleMaps

// Esta clase maneja la ubicación de forma independiente
class MapLocationController: NSObject, CLLocationManagerDelegate {
    
    // Creamos una instancia compartida (Singleton)
    static let shared = MapLocationController()
    
    let locationManager = CLLocationManager()
    var mapView: GMSMapView? // Aquí guardaremos la referencia al mapa
    
    override init() {
        super.init()
        locationManager.delegate = self
    }
    
    func requestPermissionsAndStart() {
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
    }
    
    // El delegado que escucha cuando el GPS encuentra tu ubicación
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.first, let map = mapView else { return }
        
        // Animamos la cámara
        map.animate(toLocation: location.coordinate)
        map.animate(toZoom: 15)
        
        // Detenemos la búsqueda para que el usuario pueda mover el mapa libremente
        manager.stopUpdatingLocation()
    }
}
