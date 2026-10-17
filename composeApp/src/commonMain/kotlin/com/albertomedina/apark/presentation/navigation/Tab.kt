package com.albertomedina.apark.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.tab_account
import apark.composeapp.generated.resources.tab_map
import apark.composeapp.generated.resources.tab_vehicles
import org.jetbrains.compose.resources.StringResource

/**
 * Las tres pestañas, en orden.
 *
 * `MAP` responde a **dónde está** y `VEHICLES` a **cómo está** — esa es la línea que impide que la
 * segunda duplique el carrusel de la primera. Ver `docs/NAVIGATION.md`.
 */
enum class Tab(val label: StringResource, val icon: ImageVector, val root: Destiny) {
    MAP(Res.string.tab_map, Icons.Default.Map, Home),
    VEHICLES(Res.string.tab_vehicles, Icons.Default.DirectionsCar, Vehicles),
    ACCOUNT(Res.string.tab_account, Icons.Default.Person, Account);

    companion object {
        /** La primera es a la que vuelve el botón atrás desde cualquier otra. */
        val Default = MAP
    }
}
