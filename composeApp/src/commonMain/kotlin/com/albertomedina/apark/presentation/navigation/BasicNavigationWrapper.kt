package com.albertomedina.apark.presentation.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.albertomedina.apark.presentation.auth.login.LoginScreen
import com.albertomedina.apark.presentation.auth.register.RegisterScreen
import com.albertomedina.apark.presentation.auth.resetPassword.ResetPasswordScreen
import com.albertomedina.apark.presentation.addvehicle.AddVehicleScreen
import com.albertomedina.apark.presentation.auth.verification.EmailVerificationScreen
import com.albertomedina.apark.presentation.home.HomeScreen
import com.albertomedina.apark.presentation.splash.SplashScreen
import com.albertomedina.apark.presentation.vehicledetail.VehicleDetailScreen

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface Destiny: NavKey
@Serializable
data object Splash: Destiny
@Serializable
data object Login: Destiny
@Serializable
data object Register: Destiny
@Serializable
data object ResetPassword: Destiny
@Serializable
data object VerifyEmail: Destiny
@Serializable
data object Home: Destiny
@Serializable
data object AddVehicle: Destiny
// First destination carrying a parameter: the rest are data objects.
@Serializable
data class VehicleDetail(val vehicleId: String): Destiny

// Creates the required serialization configuration for open polymorphism
private val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Splash::class, Splash.serializer())
            subclass(Login::class, Login.serializer())
            subclass(Register::class, Register.serializer())
            subclass(ResetPassword::class, ResetPassword.serializer())
            subclass(VerifyEmail::class, VerifyEmail.serializer())
            subclass(Home::class, Home.serializer())
            subclass(AddVehicle::class, AddVehicle.serializer())
            subclass(VehicleDetail::class, VehicleDetail.serializer())
        }
    }
}

@Composable
fun BasicNavigationWrapper(){

    val backStack = rememberNavBackStack(
        config,
        Splash
    )

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when(key){
                is Splash -> NavEntry(key) {
                    SplashScreen(
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(Home)
                        },
                        onNavigateToLogin = {
                            backStack.clear()
                            backStack.add(Login)
                        }
                    )
                }
                is Login -> NavEntry(key) {
                    LoginScreen(
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(Home)
                        },
                        onNavigateToVerify = {
                            backStack.add(VerifyEmail)
                        },
                        onNavigateToResetPassword = {
                            backStack.add(ResetPassword)
                        },
                        onNavigateToRegister = {
                            backStack.add(Register)
                        }
                    )
                }
                is Register -> NavEntry(key) {
                    RegisterScreen(
                        onNavigateToVerify = {
                            backStack.add(VerifyEmail)
                        },
                        onBack = {
                            backStack.removeLastOrNull()
                        }
                    )
                }
                is Home -> NavEntry(key) {
                    HomeScreen(
                        onNavigateToLogin = {
                            backStack.clear()
                            backStack.add(Login)
                        },
                        onNavigateToDetails = { vehicleId ->
                            backStack.add(VehicleDetail(vehicleId))
                        },
                        onNavigateToAddVehicle = {
                            backStack.add(AddVehicle)
                        }
                    )
                }
                is VehicleDetail -> NavEntry(key) {
                    VehicleDetailScreen(
                        vehicleId = key.vehicleId,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                is AddVehicle -> NavEntry(key) {
                    AddVehicleScreen(
                        onBack = {
                            backStack.removeLastOrNull()
                        }
                    )
                }
                is VerifyEmail -> NavEntry(key) {
                    EmailVerificationScreen(
                        onNavigateToLogin = {
                            backStack.add(Login)
                        },
                        onNavigateToHome = {
                            backStack.clear()
                            backStack.add(Home)
                        }
                    )
                }
                is ResetPassword -> NavEntry(key) {
                    ResetPasswordScreen {
                        backStack.removeLastOrNull()
                    }
                }
                else -> error("Unknown key: $key")
            }
        }
    )
}
