package com.albertomedina.apark

import android.app.Application
import com.albertomedina.apark.data.settings.androidSettingsContext
import com.albertomedina.apark.di.androidModule
import com.albertomedina.apark.di.initKoin
import org.koin.android.ext.koin.androidContext

class AparkApp: Application() {
    override fun onCreate() {
        super.onCreate()
        // Antes de arrancar Koin: el DataStore se construye al resolverlo y necesita saber dónde
        // puede escribir. En iOS esa ruta la da el sistema y no hace falta nada equivalente.
        androidSettingsContext = applicationContext

        initKoin(){
            androidContext(this@AparkApp)
            modules(androidModule)
        }
    }
}
