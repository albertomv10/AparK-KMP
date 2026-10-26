package com.albertomedina.apark.presentation.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Cada destino declara si quiere la barra de pestañas.
 *
 * Lo declara el destino y no una lista en el shell a propósito: una lista aparte es algo de lo que
 * hay que acordarse al añadir una pantalla, y de lo que nadie se acuerda.
 *
 * Las pantallas de autenticación no necesitan decir nada — viven en la capa de fuera, donde no hay
 * barra que mostrar.
 */
@Serializable
sealed interface Destiny : NavKey {
    val showsTabBar: Boolean get() = true
}

// --- Capa de fuera: autenticación, y el shell ---
@Serializable
data object Splash : Destiny
@Serializable
data object Login : Destiny
@Serializable
data object Register : Destiny
@Serializable
data object ResetPassword : Destiny
@Serializable
data object VerifyEmail : Destiny

/** La app con pestañas, como un único destino de la pila de fuera. */
@Serializable
data object Tabs : Destiny

// --- Dentro de las pestañas ---
@Serializable
data object Home : Destiny
@Serializable
data object Vehicles : Destiny
@Serializable
data object Account : Destiny

/** El detalle **conserva** la barra: quitarla al entrar da sensación de haberse salido de la app. */
@Serializable
data class VehicleDetail(val vehicleId: String) : Destiny

/** Tarea de pantalla completa, con principio y fin: sin barra. */
@Serializable
data object AddVehicle : Destiny {
    override val showsTabBar: Boolean get() = false
}

/**
 * Cambiar el email o la contraseña: tareas con principio y fin, y con teclado, así que sin barra.
 * Una pantalla y no un diálogo porque la contraseña lleva tres campos (spec 011).
 */
@Serializable
data object ChangeEmail : Destiny {
    override val showsTabBar: Boolean get() = false
}

@Serializable
data object ChangePassword : Destiny {
    override val showsTabBar: Boolean get() = false
}

/** Serialización de polimorfismo abierto, necesaria para guardar y restaurar las pilas. */
internal val navigationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Splash::class, Splash.serializer())
            subclass(Login::class, Login.serializer())
            subclass(Register::class, Register.serializer())
            subclass(ResetPassword::class, ResetPassword.serializer())
            subclass(VerifyEmail::class, VerifyEmail.serializer())
            subclass(Tabs::class, Tabs.serializer())
            subclass(Home::class, Home.serializer())
            subclass(Vehicles::class, Vehicles.serializer())
            subclass(Account::class, Account.serializer())
            subclass(AddVehicle::class, AddVehicle.serializer())
            subclass(VehicleDetail::class, VehicleDetail.serializer())
            subclass(ChangeEmail::class, ChangeEmail.serializer())
            subclass(ChangePassword::class, ChangePassword.serializer())
        }
    }
}
