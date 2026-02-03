import SwiftUI
import ComposeApp
import FirebaseCore

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
                
                // 1. Detectamos el modo usando el compilador de Swift
                #if DEBUG
                    let isDebug = true
                #else
                    let isDebug = false
                #endif
    
        
        HelperKt.doInitKoinIos(isDebug: isDebug)

    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
