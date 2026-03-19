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
            // Intentamos leer la última ubicación conocida de iOS de forma instantánea
            if let lastKnownLocation = MapLocationController.shared.locationManager.location {
                // Si la tenemos, inicializamos el mapa directamente con ese centro y zoom 15
                options.camera = GMSCameraPosition.camera(withTarget: lastKnownLocation.coordinate, zoom: 15)
            }
            let mapView = GMSMapView(options: options)
            // Habilita el punto azul de ubicación actual
            mapView.isMyLocationEnabled = true
            mapView.settings.myLocationButton = false
            
            // 4. Le pasamos el mapa recién creado a nuestro controlador
            MapLocationController.shared.mapView = mapView
            
            return mapView
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
