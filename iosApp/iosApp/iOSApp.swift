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
        FirebaseApp.configure()
        
        // 1. Detectamos el modo usando el compilador de Swift
        #if DEBUG
        let isDebug = true
        #else
        let isDebug = false
        #endif
        
        
        HelperKt.doInitKoinIos(isDebug: isDebug)
        
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
        // 1. Mover la cámara
        AparKMap_iosKt.iosMapUpdateCamera = { lat, lng, animated in
            // TRUCO PRO: DispatchQueue.main.async hace que esta orden espere
            // un milisegundo a que el mapa ya tenga su tamaño real en pantalla.
            DispatchQueue.main.async {
                if let map = MapLocationController.shared.mapView {
                    
                    let camera = GMSCameraPosition.camera(withLatitude: lat.doubleValue, longitude: lng.doubleValue, zoom: 15.0)
                    
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
                
                for marker in markers {
                    let position = CLLocationCoordinate2D(latitude: marker.lat, longitude: marker.lng)
                    let gmsMarker = GMSMarker(position: position)
                    gmsMarker.title = marker.title
                    gmsMarker.map = map
                }
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
