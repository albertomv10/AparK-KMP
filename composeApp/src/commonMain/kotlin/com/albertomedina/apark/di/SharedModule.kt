package com.albertomedina.apark.di

import com.albertomedina.apark.data.logging.FirebaseCrashReporter
import com.albertomedina.apark.data.settings.DataStoreSettingsRepository
import com.albertomedina.apark.data.settings.createSettingsDataStore
import com.albertomedina.apark.data.repository.FirebaseAuthRepository
import com.albertomedina.apark.data.repository.FirebaseInviteRepository
import com.albertomedina.apark.data.repository.FirestoreUserRepository
import com.albertomedina.apark.data.repository.FirestoreVehicleRepository
import com.albertomedina.apark.data.repository.LocationRepositoryImpl
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.settings.SettingsRepository
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.repository.InviteRepository
import com.albertomedina.apark.domain.repository.LocationRepository
import com.albertomedina.apark.domain.repository.UserRepository
import com.albertomedina.apark.domain.repository.VehicleRepository
import com.albertomedina.apark.domain.usecase.ChangeEmailUseCase
import com.albertomedina.apark.domain.usecase.ChangePasswordUseCase
import com.albertomedina.apark.domain.usecase.CreateVehicleInviteUseCase
import com.albertomedina.apark.domain.usecase.CreateVehicleUseCase
import com.albertomedina.apark.domain.usecase.DeleteVehicleUseCase
import com.albertomedina.apark.domain.usecase.GetCurrentLocationUseCase
import com.albertomedina.apark.domain.usecase.GetLastVehicleLocationUseCase
import com.albertomedina.apark.domain.usecase.GetUserUseCase
import com.albertomedina.apark.domain.usecase.GetVehicleByIdUseCase
import com.albertomedina.apark.domain.usecase.GetVehicleListUseCase
import com.albertomedina.apark.domain.usecase.JoinVehicleWithCodeUseCase
import com.albertomedina.apark.domain.usecase.LoginAppleUseCase
import com.albertomedina.apark.domain.usecase.LoginGoogleUseCase
import com.albertomedina.apark.domain.usecase.LoginUseCase
import com.albertomedina.apark.domain.usecase.MoveVehicleUseCase
import com.albertomedina.apark.domain.usecase.RegisterUseCase
import com.albertomedina.apark.domain.usecase.RemoveUserFromVehicleUseCase
import com.albertomedina.apark.domain.usecase.SignOutUseCase
import com.albertomedina.apark.domain.usecase.SyncUserProfileUseCase
import com.albertomedina.apark.domain.usecase.UpdateUserNameUseCase
import com.albertomedina.apark.domain.usecase.UpdateVehicleLocationUseCase
import com.albertomedina.apark.domain.usecase.UpdateVehicleUseCase
import com.albertomedina.apark.presentation.auth.login.LoginViewModel
import com.albertomedina.apark.presentation.auth.register.RegisterViewModel
import com.albertomedina.apark.presentation.auth.resetPassword.ResetPassWordViewmodel
import com.albertomedina.apark.presentation.auth.verification.EmailVerificationViewModel
import com.albertomedina.apark.presentation.addvehicle.AddVehicleViewModel
import com.albertomedina.apark.presentation.home.HomeViewModel
import com.albertomedina.apark.presentation.account.AccountViewModel
import com.albertomedina.apark.presentation.account.ChangeEmailViewModel
import com.albertomedina.apark.presentation.account.ChangePasswordViewModel
import com.albertomedina.apark.presentation.navigation.SessionViewModel
import com.albertomedina.apark.presentation.splash.SplashViewModel
import com.albertomedina.apark.presentation.vehicles.VehiclesViewModel
import com.albertomedina.apark.presentation.vehicledetail.VehicleDetailViewModel
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.FirebaseFunctions
import dev.gitlive.firebase.functions.functions
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val sharedModule = module {

    // Development and production are separate Firebase projects, each with a single `(default)`
    // database, so which one the app talks to is decided by the `google-services.json` /
    // `GoogleService-Info.plist` of the build variant — not by anything at runtime.
    // `useEmulator` hay que llamarlo ANTES del primer uso del SDK —cambiarle los ajustes en
    // caliente lanza—, y este es el único sitio donde se construyen: Koin los crea perezosamente,
    // justo antes de que alguien los pida. Con `emulatorHost` a null, que es lo normal, esto es
    // exactamente lo que había.
    single<FirebaseFirestore> {
        Firebase.firestore.apply {
            emulatorHost?.let { useEmulator(it, EmulatorPorts.FIRESTORE) }
        }
    }
    single<FirebaseAuth> {
        Firebase.auth.apply {
            emulatorHost?.let { useEmulator(it, EmulatorPorts.AUTH) }
        }
    }

    // The region is not optional: the callables are deployed in europe-west4, and without this
    // the SDK would call us-central1 and fail.
    single<FirebaseFunctions> {
        Firebase.functions(region = "europe-west4").apply {
            emulatorHost?.let { useEmulator(it, EmulatorPorts.FUNCTIONS) }
        }
    }

    single<CrashReporter> { FirebaseCrashReporter() }

    // Un único DataStore por proceso: abrir dos sobre el mismo fichero lanza en tiempo de ejecución.
    single { createSettingsDataStore() }
    single<SettingsRepository> { DataStoreSettingsRepository(dataStore = get()) }

    single<VehicleRepository> { FirestoreVehicleRepository(firestore = get()) }
    single<UserRepository> { FirestoreUserRepository(firestore = get()) }

    single<AuthRepository> { FirebaseAuthRepository(firebaseAuth = get()) }
    single<InviteRepository> { FirebaseInviteRepository(functions = get()) }

    single<LocationRepository> {
        LocationRepositoryImpl(
        locationSource = get()
    ) }

    // =============================
    // USE CASES (Factories)
    // =============================

    factory { GetVehicleListUseCase(repository = get()) }
    factory { CreateVehicleUseCase(repository = get()) }
    factory { DeleteVehicleUseCase(repository = get()) }
    factory { MoveVehicleUseCase(repository = get()) }
    factory { CreateVehicleInviteUseCase(repository = get()) }
    factory { JoinVehicleWithCodeUseCase(repository = get()) }
    factory { GetVehicleByIdUseCase(repository = get()) }
    factory { UpdateVehicleUseCase(repository = get()) }
    factory { RemoveUserFromVehicleUseCase(repository = get()) }
    factory { GetLastVehicleLocationUseCase(repository = get()) }
    factory { UpdateVehicleLocationUseCase(
        vehicleRepository = get(),
        locationRepository = get(),
        authRepository = get(),
        userRepository = get()
        )
    }

    // Auth
    factory { LoginUseCase(authRepository = get()) }
    factory { RegisterUseCase(authRepository = get()) }
    factory { LoginGoogleUseCase(authRepository = get()) }
    factory { LoginAppleUseCase(authRepository = get()) }
    factory { SignOutUseCase(authRepository = get()) }

    // User
    factory { GetUserUseCase(repository = get()) }
    factory { UpdateUserNameUseCase(authRepository = get(), userRepository = get()) }
    factory { SyncUserProfileUseCase(authRepository = get(), userRepository = get()) }
    factory { ChangeEmailUseCase(authRepository = get()) }
    factory { ChangePasswordUseCase(authRepository = get()) }

    // Location
    factory { GetCurrentLocationUseCase(repository = get()) }

    viewModel { SplashViewModel(authRepository = get()) }

    viewModel {
        VehiclesViewModel(
            authRepository = get(),
            getVehicleListUseCase = get()
        )
    }

    viewModel {
        AccountViewModel(
            authRepository = get(),
            getUserUseCase = get(),
            updateUserNameUseCase = get(),
            syncUserProfileUseCase = get(),
            signOutUseCase = get(),
            settingsRepository = get()
        )
    }

    viewModel {
        ChangeEmailViewModel(
            authRepository = get(),
            changeEmailUseCase = get(),
            crashReporter = get()
        )
    }

    viewModel {
        ChangePasswordViewModel(
            changePasswordUseCase = get(),
            crashReporter = get()
        )
    }

    viewModel {
        SessionViewModel(
            authRepository = get(),
            syncUserProfileUseCase = get()
        )
    }

    viewModel {
        LoginViewModel(
            loginUseCase = get(),
            loginGoogleUseCase = get(),
            loginAppleUseCase = get(),
            userRepository = get(),
            crashReporter = get()
        )
    }

    viewModel {
        RegisterViewModel(
            registerUseCase = get(),
            crashReporter = get()
        )
    }

    viewModel {
        EmailVerificationViewModel(
            authRepository = get(),
            userRepository = get()
        )
    }

    viewModel {
        ResetPassWordViewmodel(
            authRepository = get()
        )
    }

    viewModel {
        HomeViewModel(
            authRepository = get(),
            updateVehicleLocationUseCase = get(),
            getVehicleListUseCase = get(),
            deleteVehicleUseCase = get(),
            removeUserFromVehicleUseCase = get(),
            moveVehicleUseCase = get(),
            crashReporter = get()
        )
    }

    viewModel {
        AddVehicleViewModel(
            createVehicleUseCase = get(),
            joinVehicleWithCodeUseCase = get(),
            authRepository = get()
        )
    }

    viewModel {
        VehicleDetailViewModel(
            getVehicleByIdUseCase = get(),
            createVehicleInviteUseCase = get(),
            authRepository = get()
        )
    }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(sharedModule)
}
