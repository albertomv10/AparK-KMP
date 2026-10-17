package com.albertomedina.apark.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import com.albertomedina.apark.presentation.account.AccountScreen
import com.albertomedina.apark.presentation.addvehicle.AddVehicleScreen
import com.albertomedina.apark.presentation.components.AparkBottomNavigationBar
import com.albertomedina.apark.presentation.home.HomeScreen
import com.albertomedina.apark.presentation.vehicledetail.VehicleDetailScreen
import com.albertomedina.apark.presentation.vehicles.VehiclesScreen

/**
 * La app con pestañas: **una pila de navegación por pestaña**, y al `NavDisplay` se le pasa la de la
 * pestaña activa.
 *
 * Esto funciona porque en Navigation 3 la pila es tuya — `NavBackStack` es una `MutableList` sobre
 * un `SnapshotStateList`—, así que tres pestañas son tres listas y no hace falta soporte especial.
 *
 * **Consecuencia aceptada a sabiendas**: el *pop* se detecta comparando contra la lista que se está
 * mostrando, así que al cambiar de pestaña las entradas de la anterior parecen popeadas y sus
 * ViewModels se limpian. Se vuelve igualmente a la pantalla correcta —la lista se conserva— y el
 * estado se reconstruye solo, porque todo deriva de flujos de Firestore que se resuscriben desde la
 * caché local. Silenciarlo con `removeViewModelStoreOnPop = { false }` reintroduciría el bug de la
 * spec 006, así que no se hace. Ver `docs/specs/009-tab-navigation/design.md`.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TabShell(onSignedOut: () -> Unit) {
    // Tres llamadas separadas y no un bucle: `rememberNavBackStack` no acepta clave y se apoya en la
    // posición en la composición, de modo que en un bucle las tres compartirían posición y
    // colisionarían al restaurar.
    val mapStack = rememberNavBackStack(navigationConfig, Home)
    val vehiclesStack = rememberNavBackStack(navigationConfig, Vehicles)
    val accountStack = rememberNavBackStack(navigationConfig, Account)

    var currentTab by rememberSaveable { mutableStateOf(Tab.Default) }

    // Entrar en una pantalla es avanzar, y el deslizamiento lateral lo cuenta bien. Cambiar de
    // pestaña no es avanzar: son tres sitios al mismo nivel, y deslizar uno sobre otro sugiere una
    // profundidad que no existe. Hay que distinguir por **qué** ha cambiado el contenido, no por
    // cuál es, así que se marca en el momento de cambiarlo.
    var isTabSwitch by remember { mutableStateOf(false) }

    fun goToTab(tab: Tab) {
        isTabSwitch = true
        currentTab = tab
    }

    val stack = when (currentTab) {
        Tab.MAP -> mapStack
        Tab.VEHICLES -> vehiclesStack
        Tab.ACCOUNT -> accountStack
    }

    // Desde cualquier pestaña que no sea la primera, atrás vuelve a ella en vez de cerrar la app.
    // Solo en su raíz: dentro de una pestaña, atrás es navegación normal y lo maneja el NavDisplay.
    BackHandler(enabled = currentTab != Tab.Default && stack.size == 1) {
        goToTab(Tab.Default)
    }

    val showsTabBar = (stack.lastOrNull() as? Destiny)?.showsTabBar ?: true

    Scaffold(
        bottomBar = {
            if (showsTabBar) {
                AparkBottomNavigationBar(
                    current = currentTab,
                    onSelect = { tab ->
                        // Tocar la pestaña en la que ya estás te devuelve a su raíz, que es lo que
                        // espera todo el mundo y la salida de emergencia si te has perdido dentro.
                        if (tab == currentTab) {
                            isTabSwitch = false
                            while (stack.size > 1) stack.removeLastOrNull()
                        } else {
                            goToTab(tab)
                        }
                    }
                )
            }
        }
    ) { padding ->
        // Las pantallas traen su propio Scaffold, que por defecto vuelve a reservar sitio para las
        // barras del sistema — y ese sitio ya lo ha descontado este de fuera al colocar la barra de
        // pestañas. Sin `consumeWindowInsets` se suman dos veces: en Android el inset de gestos es
        // pequeño y apenas se nota, pero en iPhone el del indicador de inicio son ~34pt y el
        // contenido sube de más.
        val tabBarPadding = PaddingValues(bottom = padding.calculateBottomPadding())
        fun push(key: Destiny) {
            isTabSwitch = false
            stack.add(key)
        }

        fun pop() {
            isTabSwitch = false
            stack.removeLastOrNull()
        }

        NavDisplay(
            modifier = Modifier.padding(tabBarPadding).consumeWindowInsets(tabBarPadding),
            backStack = stack,
            onBack = { pop() },
            // Se delega en el de la plataforma —en iOS es el deslizamiento con la curva de UIKit—
            // y solo se sustituye cuando el cambio viene de la barra de pestañas.
            transitionSpec = { if (isTabSwitch) TabSwitchTransition else defaultTransitionSpec<NavKey>()() },
            popTransitionSpec = { if (isTabSwitch) TabSwitchTransition else defaultPopTransitionSpec<NavKey>()() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = { key ->
                when (key) {
                    is Home -> NavEntry(key) {
                        HomeScreen(
                            onNavigateToDetails = { push(VehicleDetail(it)) },
                            onNavigateToAddVehicle = { push(AddVehicle) }
                        )
                    }
                    is Vehicles -> NavEntry(key) {
                        VehiclesScreen(
                            onNavigateToDetails = { push(VehicleDetail(it)) },
                            onNavigateToAddVehicle = { push(AddVehicle) }
                        )
                    }
                    is Account -> NavEntry(key) {
                        AccountScreen(onSignedOut = onSignedOut)
                    }
                    is VehicleDetail -> NavEntry(key) {
                        VehicleDetailScreen(
                            vehicleId = key.vehicleId,
                            onBack = { pop() }
                        )
                    }
                    is AddVehicle -> NavEntry(key) {
                        AddVehicleScreen(onBack = { pop() })
                    }
                    else -> error("Destino inesperado dentro de las pestañas: $key")
                }
            }
        )
    }
}

/**
 * Un fundido corto entre pestañas: sin dirección, porque entre pestañas no hay ni adelante ni
 * atrás. Para que el cambio sea instantáneo basta con `EnterTransition.None togetherWith
 * ExitTransition.None`, pero sobre el mapa un corte seco parpadea.
 */
private val TabSwitchTransition = ContentTransform(
    targetContentEnter = fadeIn(tween(350)),
    initialContentExit = fadeOut(tween(350))
)
