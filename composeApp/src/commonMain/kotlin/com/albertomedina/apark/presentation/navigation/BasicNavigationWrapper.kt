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
import com.albertomedina.apark.presentation.home.HomeScreen

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
sealed interface Destiny: NavKey
@Serializable
data object Login: Destiny
@Serializable
data object Register: Destiny
@Serializable
data object ResetPassword: Destiny
@Serializable
data object VerifyEmail: Destiny
@Serializable
data class Home(val user: String): Destiny

// Creates the required serialization configuration for open polymorphism
private val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Login::class, Login.serializer())
            subclass(Register::class, Register.serializer())
            subclass(ResetPassword::class, ResetPassword.serializer())
            subclass(VerifyEmail::class, VerifyEmail.serializer())
            subclass(Home::class, Home.serializer())
        }
    }
}

@Composable
fun BasicNavigationWrapper(){

    val backStack = rememberNavBackStack(
        config,
        Login
    )

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when(key){
                is Login -> NavEntry(key) {
                    LoginScreen(
                        onNavigateToHome = {
                            backStack.add(Home(it))
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
                        user = key.user
                    )
                }
                is VerifyEmail -> NavEntry(key) {
                    // Pantalla temporal hasta que la crees
                    Text("Pantalla de Verificación de Email")
                }
                is ResetPassword -> NavEntry(key) {
                    // Pantalla temporal hasta que la crees
                    Text("Pantalla de Recuperar Contraseña")
                }
                else -> error("Unknown key: $key")
            }
        }
    )
}
