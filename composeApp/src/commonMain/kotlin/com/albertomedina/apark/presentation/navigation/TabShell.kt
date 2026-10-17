package com.albertomedina.apark.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
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

    val stack = when (currentTab) {
        Tab.MAP -> mapStack
        Tab.VEHICLES -> vehiclesStack
        Tab.ACCOUNT -> accountStack
    }

    // Desde cualquier pestaña que no sea la primera, atrás vuelve a ella en vez de cerrar la app.
    // Solo en su raíz: dentro de una pestaña, atrás es navegación normal y lo maneja el NavDisplay.
    BackHandler(enabled = currentTab != Tab.Default && stack.size == 1) {
        currentTab = Tab.Default
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
                            while (stack.size > 1) stack.removeLastOrNull()
                        } else {
                            currentTab = tab
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
        NavDisplay(
            modifier = Modifier.padding(tabBarPadding).consumeWindowInsets(tabBarPadding),
            backStack = stack,
            onBack = { stack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = { key ->
                when (key) {
                    is Home -> NavEntry(key) {
                        HomeScreen(
                            onNavigateToDetails = { stack.add(VehicleDetail(it)) },
                            onNavigateToAddVehicle = { stack.add(AddVehicle) }
                        )
                    }
                    is Vehicles -> NavEntry(key) {
                        VehiclesScreen(
                            onNavigateToDetails = { stack.add(VehicleDetail(it)) },
                            onNavigateToAddVehicle = { stack.add(AddVehicle) }
                        )
                    }
                    is Account -> NavEntry(key) {
                        AccountScreen(onSignedOut = onSignedOut)
                    }
                    is VehicleDetail -> NavEntry(key) {
                        VehicleDetailScreen(
                            vehicleId = key.vehicleId,
                            onBack = { stack.removeLastOrNull() }
                        )
                    }
                    is AddVehicle -> NavEntry(key) {
                        AddVehicleScreen(onBack = { stack.removeLastOrNull() })
                    }
                    else -> error("Destino inesperado dentro de las pestañas: $key")
                }
            }
        )
    }
}
