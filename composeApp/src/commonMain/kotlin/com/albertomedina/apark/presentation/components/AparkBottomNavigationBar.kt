package com.albertomedina.apark.presentation.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.albertomedina.apark.presentation.navigation.Tab
import org.jetbrains.compose.resources.stringResource

/**
 * La barra no guarda estado: la pestaña activa vive en el shell, que es quien tiene las pilas.
 */
@Composable
fun AparkBottomNavigationBar(
    current: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        Tab.entries.forEach { tab ->
            val label = stringResource(tab.label)
            NavigationBarItem(
                icon = { Icon(tab.icon, contentDescription = label) },
                label = { Text(label) },
                selected = current == tab,
                onClick = { onSelect(tab) }
            )
        }
    }
}
