package com.albertomedina.apark

import com.albertomedina.apark.di.AppConfig
import com.albertomedina.apark.di.initKoin
import com.albertomedina.apark.di.iosModule
import org.koin.dsl.module

// Añadimos el parámetro isDebug
fun initKoinIos(isDebug: Boolean) {
    initKoin {
        modules(
            // 1. Creamos un módulo al vuelo con la configuración que viene de Swift
            module {
                single { AppConfig(isDebug = isDebug) }
            },

            // 2. Módulo iOS normal
            iosModule
        )
    }
}