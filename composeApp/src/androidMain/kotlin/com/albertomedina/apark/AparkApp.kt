package com.albertomedina.apark

import android.app.Application
import com.albertomedina.apark.data.settings.androidSettingsContext
import com.albertomedina.apark.di.androidModule
import com.albertomedina.apark.di.initKoin
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.ktx.appCheck
import com.google.firebase.ktx.Firebase
import org.koin.android.ext.koin.androidContext

class AparkApp: Application() {
    override fun onCreate() {
        super.onCreate()
        // Antes de arrancar Koin: el DataStore se construye al resolverlo y necesita saber dónde
        // puede escribir. En iOS esa ruta la da el sistema y no hace falta nada equivalente.
        androidSettingsContext = applicationContext

        initAppCheck()

        initKoin(){
            androidContext(this@AparkApp)
            modules(androidModule)
        }
    }

    /**
     * App Check demuestra a Firebase que quien llama es **esta app** y no un script con la
     * configuración sacada del APK, que es pública por definición. Una vez inicializado, el resto
     * de SDK de Firebase adjuntan el token solos: no hace falta nada en el código compartido.
     *
     * Qué proveedor se instala lo decide el source set de la variante — ver [appCheckProviderFactory].
     */
    private fun initAppCheck() {
        FirebaseApp.initializeApp(this)
        Firebase.appCheck.installAppCheckProviderFactory(appCheckProviderFactory())
    }
}
