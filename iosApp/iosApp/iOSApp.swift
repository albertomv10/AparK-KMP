import SwiftUI
import ComposeApp
import FirebaseAppCheck
import FirebaseCore
import GoogleSignIn
import CoreLocation
import GoogleMaps

@main
struct iOSApp: App {
    
    static let appleAuthCoordinator = AppleAuthCoordinator()
    
    init() {
        
        //Firebase
        // App Check va ANTES de configure(): demuestra a Firebase que quien llama es esta app y no
        // un script con la configuracion sacada del binario, que es publica por definicion. Una vez
        // instalado, el resto de SDK adjuntan el token solos y no hace falta nada en Kotlin.
        //
        // En debug se usa el proveedor de depuracion, que imprime en consola un token que hay que
        // registrar a mano; App Attest no funciona en el simulador. En release, App Attest.
        #if DEBUG
        AppCheck.setAppCheckProviderFactory(AppCheckDebugProviderFactory())
        #else
        AppCheck.setAppCheckProviderFactory(AppAttestFactory())
        #endif

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

        // Los emuladores de Firebase, antes de Koin, que es quien construye los SDK.
        //
        // Va bajo #if DEBUG a proposito: es lo que garantiza que una build de release no pueda
        // encenderlos. La clave sigue existiendo en su Info.plist, pero no hay codigo compilado
        // que la lea. Ver `di/EmulatorConfig.kt`.
        #if DEBUG
        if let host = Bundle.main.object(forInfoDictionaryKey: "AparkEmulatorHost") as? String,
           !host.isEmpty {
            EmulatorConfigKt.emulatorHost = host
            print("AparK: emuladores de Firebase en \(host)")
        }
        #endif

        HelperKt.doInitKoinIos()
        
        //Google SignIn
        GoogleAuthBridgeKt.iosGoogleSignInProvider = { onSuccess, onError in
            signInWithGoogle(
                onSuccess: { token, accessToken in
                    _ = onSuccess(token, accessToken) // El "_ =" ignora el KotlinUnit que devuelve Kotlin
                },
                onError: { errorMsg, cancelled in
                    _ = onError(errorMsg, KotlinBoolean(bool: cancelled))
                }
            )
        }
        
        //Apple SignIn
        AppleSignInButton_iosKt.iosAppleSignInProvider = { onSuccess, onError in
            iOSApp.appleAuthCoordinator.startSignIn(
                onSuccess: { idToken, nonce in
                    _ = onSuccess(idToken, nonce)
                },
                onError: { errorMsg, cancelled in
                    _ = onError(errorMsg, KotlinBoolean(bool: cancelled))
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
            
            // Si Kotlin ya ha mandado adónde ir, el mapa nace ahí (spec 017). Si no, en España, como
            // Android (`DefaultMapCenter`), hasta que llegue la primera orden.
            options.camera = MapLocationController.shared.pendingCamera
                ?? GMSCameraPosition.camera(withLatitude: 40.4168, longitude: -3.7038, zoom: 5.0)
            MapLocationController.shared.pendingCamera = nil
            
            let mapView = GMSMapView(options: options)
            
            // Habilita el punto azul de ubicación actual
            mapView.isMyLocationEnabled = true
            mapView.settings.myLocationButton = false
            mapView.delegate = MapLocationController.shared
            
            // 4. Le pasamos el mapa recién creado a nuestro controlador
            MapLocationController.shared.mapView = mapView
            
            
            return mapView
        }
        
        // El tema del mapa lo decide la app, no el sistema: Kotlin manda el JSON del estilo
        // oscuro, o nil para el normal. Es el mismo JSON que usa Android, desde commonMain.
        AparKMap_iosKt.updateMapStyle = { json in
            guard let mapView = MapLocationController.shared.mapView else { return }
            applyMapStyle(mapView, json: json)
        }

        // El mapa de elegir la ubicación predeterminada (spec 024): otro mapa, con su propio
        // controlador, que nace donde dice Kotlin y le avisa del centro cada vez que se para.
        PlacePickerMap_iosKt.iosPlacePickerFactory = { lat, lng, zoom, styleJson in
            let options = GMSMapViewOptions()
            options.camera = GMSCameraPosition.camera(
                withLatitude: lat.doubleValue,
                longitude: lng.doubleValue,
                zoom: zoom.floatValue
            )
            let mapView = GMSMapView(options: options)
            mapView.isMyLocationEnabled = true
            mapView.settings.myLocationButton = false
            mapView.delegate = PlacePickerController.shared
            applyMapStyle(mapView, json: styleJson)
            return mapView
        }

        // El mapa se va de la pantalla: se suelta, para que una orden de cámara no vaya a él (spec 017).
        AparKMap_iosKt.iosOnMapReleased = { view in
            if MapLocationController.shared.mapView === view {
                MapLocationController.shared.mapView = nil
            }
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
        // 1. Mover la cámara. Adónde y cómo lo decide la regla común (spec 017); aquí solo se ejecuta.
        AparKMap_iosKt.iosMapUpdateCamera = { lat, lng, zoom, animated in
            // DispatchQueue.main.async: la orden espera a que el mapa tenga su tamaño real en pantalla.
            DispatchQueue.main.async {
                guard let map = MapLocationController.shared.mapView else {
                    // El mapa aún no existe: que nazca ya aquí. Sin zoom que conservar, el de calle.
                    MapLocationController.shared.pendingCamera = GMSCameraPosition.camera(
                        withLatitude: lat.doubleValue,
                        longitude: lng.doubleValue,
                        zoom: zoom?.floatValue ?? 17
                    )
                    return
                }
                // Sin zoom, se conserva el que tenga quien mira.
                let camera = GMSCameraPosition.camera(
                    withLatitude: lat.doubleValue,
                    longitude: lng.doubleValue,
                    zoom: zoom?.floatValue ?? map.camera.zoom
                )
                // Siempre con `animate`, también el salto: a 0 s, Google Maps respeta la orden aunque
                // el mapa acabe de nacer, cosa que `map.camera = camera` no siempre hace.
                CATransaction.begin()
                if animated.boolValue {
                    CATransaction.setAnimationDuration(0.8)
                    CATransaction.setAnimationTimingFunction(CAMediaTimingFunction(name: .easeInEaseOut))
                } else {
                    CATransaction.setAnimationDuration(0.0)
                }
                map.animate(to: camera)
                CATransaction.commit()
            }
        }

        // 2. Pintar los marcadores
        AparKMap_iosKt.iosMapUpdateMarkers = { markers in
            if let map = MapLocationController.shared.mapView {
                map.clear() // Borra los pines antiguos para no duplicarlos
                MapLocationController.shared.markers = []
                
                for marker in markers {
                    let position = CLLocationCoordinate2D(latitude: marker.lat, longitude: marker.lng)
                    let gmsMarker = GMSMarker(position: position)
                    let vehicleIndex = Int(marker.vehicleIndex)
                    let selectedVehicleIndex = Int(marker.selectedVehicleIndex)
                    let colorToUse = if(selectedVehicleIndex == vehicleIndex) { UIColor.systemRed } else { UIColor.systemBlue}
                    
                    // En su ubicación predeterminada, el mismo pin con una casa (spec 024).
                    gmsMarker.icon = marker.atPlace
                        ? markerImageAtPlace(color: colorToUse)
                        : GMSMarker.markerImage(with: colorToUse)
                    gmsMarker.title = marker.title
                    // Solo se arrastra el del vehículo elegido (spec 024): para mover otro, se elige
                    // antes, en el carrusel o tocando su pin.
                    gmsMarker.isDraggable = selectedVehicleIndex == vehicleIndex
                    gmsMarker.userData = marker.id
                    gmsMarker.map = map
                    MapLocationController.shared.markers.append(gmsMarker)
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


/// Fabrica de App Attest.
///
/// Hay que escribirla: el SDK trae `AppCheckDebugProviderFactory` y `DeviceCheckProviderFactory`
/// hechas, pero **App Attest no tiene fabrica propia** — solo el proveedor. No hace falta comprobar
/// la version del sistema porque el target minimo del proyecto es iOS 18.2, muy por encima del 14
/// que App Attest requiere.
private final class AppAttestFactory: NSObject, AppCheckProviderFactory {
    func createProvider(with app: FirebaseApp) -> AppCheckProvider? {
        AppAttestProvider(app: app)
    }
}

// Lógica nativa de iOS para abrir la ventana de Google
// `onError` informa además de si el usuario canceló: cerrar el diálogo no es un fallo y no debe
// producir ningún mensaje. Desde Kotlin no se puede distinguir —solo llegaría el texto localizado
// del error—, así que la decisión se toma aquí, donde sí existe el código.
func signInWithGoogle(onSuccess: @escaping (String, String) -> Void, onError: @escaping (String, Bool) -> Void) {
    // 1. Buscamos el ViewController principal para poder mostrar la alerta encima
    guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
          let rootViewController = windowScene.windows.first?.rootViewController else {
        onError("No se pudo encontrar la ventana principal de iOS", false)
        return
    }
    
    // 2. Llamamos al SDK de Google
    GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController) { signInResult, error in
        if let error = error {
            let cancelled = (error as NSError).code == GIDSignInError.canceled.rawValue
            onError(error.localizedDescription, cancelled)
            return
        }
        
        // 3. Extraemos el JWT (Token)
        guard let idToken = signInResult?.user.idToken?.tokenString,
              let accessToken = signInResult?.user.accessToken.tokenString else {
            onError("No se pudo obtener el token de Google", false)
            return
        }
        
        // 4. ¡Se lo enviamos de vuelta a Kotlin!
        onSuccess(idToken, accessToken)
    }
}
