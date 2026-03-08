package com.albertomedina.apark.di

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