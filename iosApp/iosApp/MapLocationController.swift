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

    /// Los marcadores que hay ahora en el mapa, para saber si una pulsación larga cae encima de uno.
    var markers: [GMSMarker] = []
    
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

    /// Mantener pulsado el mapa vacío mueve ahí el vehículo elegido (spec 017).
    ///
    /// Sobre un marcador, el mismo gesto empieza a arrastrarlo, pero en iOS el SDK avisa **también**
    /// de la pulsación larga: el vehículo se movía al punto del dedo, los marcadores se repintaban y el
    /// arrastre se cortaba (spec 024). Así que una pulsación encima de un marcador se ignora aquí y se
    /// queda para el arrastre. En Android no pasa: allí una pulsación sobre un marcador no llega al mapa.
    func mapView(_ mapView: GMSMapView, didLongPressAt coordinate: CLLocationCoordinate2D) {
        let pressed = mapView.projection.point(for: coordinate)
        if markers.contains(where: { isOnMarker($0, point: pressed, in: mapView) }) { return }
        AparKMap_iosKt.iosOnMapLongPressed?(
            KotlinDouble(value: coordinate.latitude),
            KotlinDouble(value: coordinate.longitude)
        )
    }

    /// Si un punto de la pantalla cae sobre la imagen de un marcador, con un margen para el dedo. La
    /// imagen cuelga de su ancla, que en un pin es la punta, abajo en el centro.
    private func isOnMarker(_ marker: GMSMarker, point: CGPoint, in mapView: GMSMapView) -> Bool {
        guard let icon = marker.icon else { return false }
        let anchor = mapView.projection.point(for: marker.position)
        let frame = CGRect(
            x: anchor.x - icon.size.width * marker.groundAnchor.x,
            y: anchor.y - icon.size.height * marker.groundAnchor.y,
            width: icon.size.width,
            height: icon.size.height
        )
        return frame.insetBy(dx: -8, dy: -8).contains(point)
    }
}

/// El mapa de elegir la ubicación predeterminada (spec 024). Es otro `GMSMapView`, aparte del del
/// carrusel, con su propio delegado: `MapLocationController.shared` solo conoce aquel, y mezclarlos
/// mandaría las órdenes de cámara del carrusel al mapa equivocado.
final class PlacePickerController: NSObject, GMSMapViewDelegate {

    static let shared = PlacePickerController()

    /// La cámara se ha parado: su centro es el punto elegido. Mientras se arrastra no se avisa.
    func mapView(_ mapView: GMSMapView, idleAt position: GMSCameraPosition) {
        PlacePickerMap_iosKt.iosOnPickerCenterChanged?(
            KotlinDouble(value: position.target.latitude),
            KotlinDouble(value: position.target.longitude)
        )
    }
}

/// El estilo del mapa, el mismo para los dos mapas: el JSON oscuro que manda Kotlin, o nil para el
/// normal.
///
/// `overrideUserInterfaceStyle` no es opcional. Desde la versión 9, el SDK de Maps adapta el mapa al
/// modo oscuro **del sistema** él solo, y `mapStyle = nil` quiere decir "el de por defecto", que es
/// justamente ese. Con el móvil en oscuro y la app en claro, quitar el estilo devolvía el mapa al
/// oscuro del SDK en vez de aclararlo. Fijando el rasgo, el mapa deja de mirar al sistema y hace caso
/// a la app.
func applyMapStyle(_ mapView: GMSMapView, json: String?) {
    guard let json else {
        mapView.overrideUserInterfaceStyle = .light
        mapView.mapStyle = nil
        return
    }
    mapView.overrideUserInterfaceStyle = .dark
    // El inicializador lanza si el JSON no es válido. Es un literal del propio repositorio, así que si
    // fallara sería un error de programación: se cae al estilo por defecto en vez de arrastrar un throw.
    mapView.mapStyle = try? GMSMapStyle(jsonString: json)
}

/// El pin de un vehículo aparcado en su ubicación predeterminada (spec 024): el pin de siempre, con su
/// color, y una casa blanca en la cabeza. Se dibuja encima de la imagen del SDK para que sea
/// exactamente el mismo pin que el de los demás.
func markerImageAtPlace(color: UIColor) -> UIImage {
    let pin = GMSMarker.markerImage(with: color)
    let glyphSize = pin.size.width * 0.5
    let configuration = UIImage.SymbolConfiguration(pointSize: glyphSize, weight: .bold)
    guard let house = UIImage(systemName: "house.fill", withConfiguration: configuration)?
        .withTintColor(.white, renderingMode: .alwaysOriginal) else { return pin }

    let renderer = UIGraphicsImageRenderer(size: pin.size)
    return renderer.image { _ in
        pin.draw(at: .zero)
        // La cabeza del pin es un círculo en la parte de arriba, de su mismo ancho.
        let houseSize = house.size
        let origin = CGPoint(
            x: (pin.size.width - houseSize.width) / 2,
            y: pin.size.width / 2 - houseSize.height / 2 - pin.size.width * 0.04
        )
        house.draw(at: origin)
    }
}
