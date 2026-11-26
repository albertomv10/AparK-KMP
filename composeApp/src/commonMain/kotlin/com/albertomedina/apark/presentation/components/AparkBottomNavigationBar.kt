package com.albertomedina.apark.presentation.components

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.tab_badge_incidents
import com.albertomedina.apark.presentation.navigation.Tab
import org.jetbrains.compose.resources.stringResource

/**
 * La barra no guarda estado: la pestaña activa vive en el shell, que es quien tiene las pilas.
 *
 * @param badges la chapita de cada pestaña que la lleva (spec 020): hoy, las incidencias abiertas en
 *   *Vehículos*. Con `0`, o sin entrada, no hay chapita.
 */
@Composable
fun AparkBottomNavigationBar(
    current: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
    badges: Map<Tab, Int> = emptyMap()
) {
    NavigationBar(modifier = modifier) {
        Tab.entries.forEach { tab ->
            val label = stringResource(tab.label)
            val count = badges[tab] ?: 0
            val badgeDescription = if (count > 0) stringResource(Res.string.tab_badge_incidents, count) else null
            NavigationBarItem(
                icon = {
                    BadgedBox(
                        badge = {
                            if (count > 0) {
                                Badge { Text(if (count > 99) "99+" else count.toString()) }
                            }
                        }
                    ) {
                        Icon(tab.icon, contentDescription = label)
                    }
                },
                // Lo que se anuncia va en la etiqueta: con etiqueta, `NavigationBarItem` borra la
                // semántica del icono —para no leer dos veces el nombre—, y con ella la de la chapita.
                // Así se oye «Vehículos, Incidencias abiertas: 3».
                label = {
                    Text(
                        label,
                        modifier = if (badgeDescription == null) Modifier
                        else Modifier.semantics { contentDescription = "$label, $badgeDescription" }
                    )
                },
                selected = current == tab,
                onClick = { onSelect(tab) }
            )
        }
    }
}
