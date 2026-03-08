import SwiftUI
import ComposeApp
import FirebaseCore
import GoogleSignIn

@main
struct iOSApp: App {
    
    static let appleAuthCoordinator = AppleAuthCoordinator()
    
    init() {
        FirebaseApp.configure()
                
                // 1. Detectamos el modo usando el compilador de Swift
                #if DEBUG
                    let isDebug = true
                #else
                    let isDebug = false
                #endif
    
        
        HelperKt.doInitKoinIos(isDebug: isDebug)
        
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
