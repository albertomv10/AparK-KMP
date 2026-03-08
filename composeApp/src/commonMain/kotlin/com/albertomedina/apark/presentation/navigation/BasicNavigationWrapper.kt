package com.albertomedina.apark.presentation.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.albertomedina.apark.presentation.auth.login.LoginScreen
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
        onBack = {backStack.removeLastOrNull()},
        entryProvider = { key ->
            when(key){
                is Login -> NavEntry(key) {
                    LoginScreen(
                        onNavigateToHome = {
                            backStack.add(Home(it))
                        },
                        onNavigateToVerify = {TODO()},

                        onNavigateToResetPassword = {TODO()},
                        onNavigateToRegister = {TODO()}

                    )
                }
                is Home -> NavEntry(key) {
                    HomeScreen(
                        user = key.user
                    )
                }
                else -> error("Unknown key: $key")
            }

        }
    )

}