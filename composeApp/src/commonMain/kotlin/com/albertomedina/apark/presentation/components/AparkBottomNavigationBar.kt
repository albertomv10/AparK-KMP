package com.albertomedina.apark.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable
fun AparkBottomNavigationBar(
    modifier: Modifier = Modifier
) {
    // Estado local solo para que la barra reaccione visualmente al tocarla
    var selectedItem by remember { mutableIntStateOf(0) }

    val items = listOf("Mapa", "Mis Coches", "Perfil")
    val icons = listOf(Icons.Default.Map, Icons.Default.DirectionsCar, Icons.Default.Person)

    NavigationBar(
        modifier = modifier
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
    }
}