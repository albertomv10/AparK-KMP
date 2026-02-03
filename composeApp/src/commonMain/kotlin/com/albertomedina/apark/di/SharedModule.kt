package com.albertomedina.apark.di

import com.albertomedina.apark.TestViewModel
import com.albertomedina.apark.data.repository.LocationRepositoryImpl
import com.albertomedina.apark.domain.repository.LocationRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val sharedModule = module {

    // 1. La fuente de la verdad (El booleano que viene de Android/iOS)
    //single { AppConfig(isDebug = get()) }

    // 2. Koin decide qué Base de Datos fabricar
    single<FirebaseFirestore> {
        val config = get<AppConfig>()

        // AQUÍ está la lógica que decías.
        // Transformamos el booleano en el nombre de la BBDD.
        val dbName = if (config.isDebug) {
            "apark-at"   // Nombre de tu BD de pruebas (si tienes Blaze)
        } else {
            "(default)"   // Producción siempre es la default
        }

        // Devolvemos la instancia conectada a esa BD específica
        // Nota: Si usas plan gratuito, ignora el dbName aquí y gestiona las colecciones en el Repo,
        // pero la estructura es esta.
        try {
            Firebase.firestore(Firebase.app, dbName)
        } catch (e: Exception) {
            // Fallback por si acaso
            Firebase.firestore
        }
    }

    single<LocationRepository> { LocationRepositoryImpl(
        locationSource = get(),
        firestore = get()
    ) }
    viewModel { TestViewModel(get()) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration() // Configuraciones extra (importante para Android Context)

    // Cargamos siempre el módulo compartido.
    // Pero dejamos un hueco para pasar módulos extra (platformModules)
    modules(sharedModule)
}