import SwiftUI
import ComposeApp
import FirebaseCore
import GoogleSignIn
import CoreLocation
import GoogleMaps

@main
struct iOSApp: App {
    
    static let appleAuthCoordinator = AppleAuthCoordinator()
    
    init() {
        
        //Firebase
        // Qué proyecto (dev o prod) se usa lo decide el GoogleService-Info.plist que la fase de
        // build copia según la configuración, no nada en tiempo de ejecución.
        FirebaseApp.configure()

        // El client ID de Google sale del GoogleService-Info.plist que ha copiado la fase de
        // build, no de Info.plist. Incrustarlo alli hacia que release usase el client de debug:
        // pasaba desapercibido porque los dos vivian en el mismo proyecto y Firebase Auth
        // aceptaba el token igual. Con proyectos separados, eso ya no se sostiene.
        if let clientID = FirebaseApp.app()?.options.clientID {
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        } else {
            assertionFailure("Firebase no trae clientID: revisa el GoogleService-Info.plist copiado")
        }

        HelperKt.doInitKoinIos()
        
        //Google SignIn
        GoogleAuthBridgeKt.iosGoogleSignInProvider = { onSuccess, onError in
            signInWithGoogle(
                onSuccess: { token, accessToken in
                    _ = onSuccess(token, accessToken) // El "_ =" ignora el KotlinUnit que devuelve Kotlin
                },
                onError: { errorMsg in
                    _ = onError(errorMsg)
                }
            )
        }
        
        //Apple SignIn
        AppleSignInButton_iosKt.iosAppleSignInProvider = { onSuccess, onError in
            iOSApp.appleAuthCoordinator.startSignIn(
                onSuccess: { idToken, nonce in
                    _ = onSuccess(idToken, nonce)
                },
                onError: { errorMsg in
                    _ = onError(errorMsg)
                }
            )
        }
        
        //GoogleMap
        // 1. Proporciona tu API Key
        GMSServices.provideAPIKey(Bundle.main.object(forInfoDictionaryKey: "GoogleMapsAPIKey") as? String ?? "")
        
        // 2. Iniciamos la búsqueda de ubicación usando nuestro Singleton
        MapLocationController.shared.requestPermissionsAndStart()
        
        // 3. Pasa la fábrica del mapa a Kotlin
        AparKMap_iosKt.iosMapViewFactory = {
            let options = GMSMapViewOptions()
            
            // Le damos una cámara por defecto (ej. Madrid) para que el motor empiece a dibujar.
            // Una milésima de segundo después, el código de arriba lo teletransportará a la tarjeta real.
            options.camera = GMSCameraPosition.camera(withLatitude: 40.4168, longitude: -3.7038, zoom: 5.0)
            
            let mapView = GMSMapView(options: options)
            
            // Habilita el punto azul de ubicación actual
            mapView.isMyLocationEnabled = true
            mapView.settings.myLocationButton = false
            mapView.delegate = MapLocationController.shared
            
            // 4. Le pasamos el mapa recién creado a nuestro controlador
            MapLocationController.shared.mapView = mapView
            
            
            return mapView
        }
        
        //Escuchamos los cambios de padding que manda Kotlin
        AparKMap_iosKt.updateMapPadding = { newPadding in
                    // Convertimos el Double que manda Kotlin a CGFloat y actualizamos el mapa
                    MapLocationController.shared.mapView?.padding = UIEdgeInsets(
                        top: 0,
                        left: 0,
                        bottom: CGFloat(truncating: newPadding),
                        right: 0
                    )
                }
        
    //Mapa
        // 1. Mover la cámara
        AparKMap_iosKt.iosMapUpdateCamera = { lat, lng, zoom, animated in
            // TRUCO PRO: DispatchQueue.main.async hace que esta orden espere
            // un milisegundo a que el mapa ya tenga su tamaño real en pantalla.
            DispatchQueue.main.async {
                if let map = MapLocationController.shared.mapView {
                    
                    let camera = GMSCameraPosition.camera(withLatitude: lat.doubleValue, longitude: lng.doubleValue, zoom: zoom.floatValue)
                    
                    if animated.boolValue {
                        // MOVIMIENTO SUAVE (Al deslizar tarjetas)
                        CATransaction.begin()
                        CATransaction.setAnimationDuration(0.8)
                        CATransaction.setAnimationTimingFunction(CAMediaTimingFunction(name: .easeInEaseOut))
                        map.animate(to: camera)
                        CATransaction.commit()
                    } else {
                        // TELETRANSPORTE INICIAL
                        // En lugar de 'map.camera = camera', usamos la animación a 0.0 segundos
                        // Esto fuerza a Google Maps a respetar la orden aunque acabe de nacer.
                        CATransaction.begin()
                        CATransaction.setAnimationDuration(0.0)
                        map.animate(to: camera)
                        CATransaction.commit()
                    }
                }
            }
        }

        // 2. Pintar los marcadores
        AparKMap_iosKt.iosMapUpdateMarkers = { markers in
            if let map = MapLocationController.shared.mapView {
                map.clear() // Borra los pines antiguos para no duplicarlos
                
                let markerColors: [UIColor] = [
                            .systemBlue, .systemRed,  .systemGreen, .systemYellow,
                                .systemOrange, .cyan, .systemPink, .systemPurple, .magenta
                            ]
                
                for marker in markers {
                    let position = CLLocationCoordinate2D(latitude: marker.lat, longitude: marker.lng)
                    let gmsMarker = GMSMarker(position: position)
                    let vehicleIndex = Int(marker.vehicleIndex)
                    let selectedVehicleIndex = Int(marker.selectedVehicleIndex)
                    let colorToUse = if(selectedVehicleIndex == vehicleIndex) { UIColor.systemRed } else { UIColor.systemBlue}
                    
                    gmsMarker.icon = GMSMarker.markerImage(with: colorToUse)
                    gmsMarker.title = marker.title
                    gmsMarker.isDraggable = true
                    gmsMarker.userData = marker.id
                    gmsMarker.map = map
                }
            }
        }
        
        // 3. Centrar en el usuario
        AparKMap_iosKt.iosCenterOnUserLocation = { animated in
            DispatchQueue.main.async {
                MapLocationController.shared.centerOnUserLocation(animated: animated.boolValue)
            }
        }
        
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

// Lógica nativa de iOS para abrir la ventana de Google
func signInWithGoogle(onSuccess: @escaping (String, String) -> Void, onError: @escaping (String) -> Void) {
    // 1. Buscamos el ViewController principal para poder mostrar la alerta encima
    guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
          let rootViewController = windowScene.windows.first?.rootViewController else {
        onError("No se pudo encontrar la ventana principal de iOS")
        return
    }
    
    // 2. Llamamos al SDK de Google
    GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController) { signInResult, error in
        if let error = error {
            onError(error.localizedDescription)
            return
        }
        
        // 3. Extraemos el JWT (Token)
        guard let idToken = signInResult?.user.idToken?.tokenString,
              let accessToken = signInResult?.user.accessToken.tokenString else {
            onError("No se pudo obtener el token de Google")
            return
        }
        
        // 4. ¡Se lo enviamos de vuelta a Kotlin!
        onSuccess(idToken, accessToken)
    }
}
