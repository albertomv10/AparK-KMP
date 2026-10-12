package com.albertomedina.apark.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.albertomedina.apark.presentation.auth.login.LoginScreen
import com.albertomedina.apark.presentation.auth.register.RegisterScreen
import com.albertomedina.apark.presentation.auth.resetPassword.ResetPasswordScreen
import com.albertomedina.apark.presentation.auth.verification.EmailVerificationScreen
import com.albertomedina.apark.presentation.splash.SplashScreen

/**
 * La capa de fuera: autenticación, y el shell de pestañas como un destino más.
 *
 * Partirlo así resuelve media spec por construcción — la barra pertenece al shell, y estas
 * pantallas están fuera de él, así que no pueden mostrarla aunque se quiera. Y entrar o salir de la
 * sesión mueve **esta** pila, sin tocar las de las pestañas.
 */
@Composable
fun RootNavigation() {
    val backStack = rememberNavBackStack(navigationConfig, Splash)

    fun enterApp() {
        backStack.clear()
        backStack.add(Tabs)
    }

    fun goToLogin() {
        backStack.clear()
        backStack.add(Login)
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        // NavDisplay solo trae el decorador de SaveableStateHolder por defecto. Sin el de
        // ViewModelStore, ningún ViewModel se destruiría y cada pantalla arrastraría el estado de su
        // visita anterior; y el segundo depende del primero para repartir SavedStateHandles, así que
        // los dos tienen que ir (spec 006).
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = { key ->
            when (key) {
                is Splash -> NavEntry(key) {
                    SplashScreen(onNavigateToHome = ::enterApp, onNavigateToLogin = ::goToLogin)
                }
                is Login -> NavEntry(key) {
                    LoginScreen(
                        onNavigateToHome = { enterApp() },
                        onNavigateToVerify = { backStack.add(VerifyEmail) },
                        onNavigateToResetPassword = { backStack.add(ResetPassword) },
                        onNavigateToRegister = { backStack.add(Register) }
                    )
                }
                is Register -> NavEntry(key) {
                    RegisterScreen(
                        onNavigateToVerify = { backStack.add(VerifyEmail) },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                is VerifyEmail -> NavEntry(key) {
                    EmailVerificationScreen(
                        onNavigateToLogin = { backStack.add(Login) },
                        onNavigateToHome = ::enterApp
                    )
                }
                is ResetPassword -> NavEntry(key) {
                    ResetPasswordScreen { backStack.removeLastOrNull() }
                }
                is Tabs -> NavEntry(key) {
                    // Al cerrar sesión se vuelve aquí fuera: las pilas de las pestañas se van con el
                    // shell, así que no queda nada de la sesión anterior.
                    TabShell(onSignedOut = ::goToLogin)
                }
                else -> error("Destino inesperado en la raíz: $key")
            }
        }
    )
}
