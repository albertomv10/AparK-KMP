package com.albertomedina.apark.di

import com.albertomedina.apark.TestViewModel
import com.albertomedina.apark.data.repository.FirebaseAuthRepository
import com.albertomedina.apark.data.repository.FirestoreRepository
import com.albertomedina.apark.data.repository.LocationRepositoryImpl
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.repository.LocationRepository
import com.albertomedina.apark.domain.repository.UserRepository
import com.albertomedina.apark.domain.repository.VehicleRepository
import com.albertomedina.apark.domain.usecase.GetCurrentLocationUseCase
import com.albertomedina.apark.domain.usecase.GetLastVehicleLocationUseCase
import com.albertomedina.apark.domain.usecase.GetUserUseCase
import com.albertomedina.apark.domain.usecase.GetVehicleByIdUseCase
import com.albertomedina.apark.domain.usecase.GetVehicleListUseCase
import com.albertomedina.apark.domain.usecase.LoginGoogleUseCase
import com.albertomedina.apark.domain.usecase.LoginUseCase
import com.albertomedina.apark.domain.usecase.RegisterUseCase
import com.albertomedina.apark.domain.usecase.RemoveUserFromVehicleUseCase
import com.albertomedina.apark.domain.usecase.UpdateVehicleUseCase
import com.albertomedina.apark.presentation.auth.login.LoginViewModel
import com.albertomedina.apark.presentation.home.HomeViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
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
    single<FirebaseAuth>{ Firebase.auth }

    single<VehicleRepository> { FirestoreRepository(firestore = get()) }

    single<UserRepository> { FirestoreRepository(firestore = get()) }

    single<AuthRepository> { FirebaseAuthRepository(firebaseAuth = get()) }

    single<LocationRepository> {
        LocationRepositoryImpl(
        locationSource = get(),
        firestore = get()
    ) }

    // =============================
    // USE CASES (Factories)
    // =============================

    // Vehículos
    factory { GetVehicleListUseCase(repository = get()) }
    factory { GetVehicleByIdUseCase(repository = get()) }
    factory { UpdateVehicleUseCase(repository = get()) }
    factory { RemoveUserFromVehicleUseCase(repository = get()) }
    factory { GetLastVehicleLocationUseCase(repository = get()) }

    // Auth
    factory { LoginUseCase(authRepository = get()) }
    factory { RegisterUseCase(authRepository = get()) }
    factory { LoginGoogleUseCase(authRepository = get()) }

    // User
    factory { GetUserUseCase(repository = get()) }

    // Location
    factory { GetCurrentLocationUseCase(repository = get()) }

    //viewModel { TestViewModel(get()) }

    viewModel {
        LoginViewModel(
            loginUseCase = get(),
            loginGoogleUseCase = get(),
            userRepository = get()
        )
    }

    viewModel {
        HomeViewModel()
    }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration() // Configuraciones extra (importante para Android Context)

    // Cargamos siempre el módulo compartido.
    // Pero dejamos un hueco para pasar módulos extra (platformModules)
    modules(sharedModule)
}