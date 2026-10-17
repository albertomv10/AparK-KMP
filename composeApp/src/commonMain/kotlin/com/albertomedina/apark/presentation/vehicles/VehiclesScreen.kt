package com.albertomedina.apark.presentation.vehicles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.presentation.components.DynamicTimeText
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * La lista de vehículos: la puerta de entrada a gestionar cada uno.
 *
 * Nace sobria a propósito. Lo que justifica esta pestaña frente al carrusel del mapa es que enseñe
 * **cómo está** cada vehículo —incidencias abiertas, ITV próxima— y eso llega con las fases 2 y 3.
 * Hasta entonces se parecerá al carrusel; si cuando tengan contenido sigue sin aportar nada
 * distinto, sobra. Ver `docs/NAVIGATION.md`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesScreen(
    viewModel: VehiclesViewModel = koinViewModel(),
    onNavigateToDetails: (String) -> Unit,
    onNavigateToAddVehicle: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.tab_vehicles)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddVehicle) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.home_add_vehicle))
            }
        }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }

            state.vehicles.isEmpty() -> EmptyState(Modifier.fillMaxSize().padding(padding))

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.vehicles, key = { it.id }) { vehicle ->
                    VehicleRow(vehicle) { onNavigateToDetails(vehicle.id) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun VehicleRow(vehicle: Vehicle, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = vehicle.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val location = vehicle.lastLocation
            if (location == null || location.timestamp == 0L) {
                Text(
                    text = stringResource(Res.string.home_not_parked_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                DynamicTimeText(
                    timestamp = location.timestamp,
                    text = Res.string.home_last_time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (vehicle.licensePlate.isNotBlank()) {
                Text(
                    text = vehicle.licensePlate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(modifier.padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text(
            text = stringResource(Res.string.vehicles_empty),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}
