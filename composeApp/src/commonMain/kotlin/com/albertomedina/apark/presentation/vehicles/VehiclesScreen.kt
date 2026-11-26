package com.albertomedina.apark.presentation.vehicles

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.presentation.components.ParkedText
import com.albertomedina.apark.presentation.vehicleform.VehicleAvatar
import com.albertomedina.apark.presentation.vehicleform.hasAvatar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * La lista de vehículos: la puerta de entrada a gestionar cada uno, y el sitio donde se ordenan.
 *
 * Hoy aporta poco frente al carrusel del mapa, y se asume: lo que la justifica es que enseñe **cómo
 * está** cada vehículo —lo pendiente de todos arriba, e iconos en cada fila— cuando lleguen las
 * incidencias y los vencimientos. Ver `docs/NAVIGATION.md`.
 *
 * **Ordenar** (spec 016) es un modo: «Ordenar» enseña un asa en cada fila, se arrastra por ella y
 * «Hecho» lo guarda, una sola vez. Mientras tanto las filas no abren el detalle, para que tocar y
 * arrastrar no se pisen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun VehiclesScreen(
    viewModel: VehiclesViewModel = koinViewModel(),
    onNavigateToDetails: (String) -> Unit,
    onNavigateToAddVehicle: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val vehicles = state.shownVehicles
    val snackbarHostState = remember { SnackbarHostState() }
    val reorderError = stringResource(Res.string.reorder_error)
    val loadError = stringResource(Res.string.vehicles_load_error)

    LaunchedEffect(state.errorKey) {
        val (text, duration) = when (state.errorKey) {
            null -> return@LaunchedEffect
            // Como en *Inicio*: es lo que explica una lista vacía, así que se queda más tiempo.
            VehiclesViewModel.ERROR_LOAD_KEY -> loadError to SnackbarDuration.Long
            else -> reorderError to SnackbarDuration.Short
        }
        snackbarHostState.showSnackbar(text, withDismissAction = true, duration = duration)
        viewModel.onEvent(VehiclesEvent.MessageShown)
    }

    // Atrás termina de ordenar, igual que «Hecho»: lo movido no se pierde.
    BackHandler(enabled = state.isReordering) {
        viewModel.onEvent(VehiclesEvent.ReorderFinished)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.tab_vehicles)) },
                actions = {
                    if (state.isReordering) {
                        TextButton(onClick = { viewModel.onEvent(VehiclesEvent.ReorderFinished) }) {
                            Text(stringResource(Res.string.vehicles_reorder_done))
                        }
                    } else if (vehicles.size >= 2) {
                        TextButton(onClick = { viewModel.onEvent(VehiclesEvent.ReorderStarted) }) {
                            Text(stringResource(Res.string.vehicles_reorder))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!state.isReordering) {
                FloatingActionButton(onClick = onNavigateToAddVehicle) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.home_add_vehicle))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }

            vehicles.isEmpty() -> EmptyState(Modifier.fillMaxSize().padding(padding))

            else -> VehicleList(
                vehicles = vehicles,
                currentUserId = state.currentUserId,
                isReordering = state.isReordering,
                onOpen = onNavigateToDetails,
                onMove = { from, to -> viewModel.onEvent(VehiclesEvent.Moved(from, to)) },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}

@Composable
private fun VehicleList(
    vehicles: List<Vehicle>,
    currentUserId: String?,
    isReordering: Boolean,
    onOpen: (String) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier
) {
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    // Cada fila es un solo elemento de la lista, con su separador dentro, así que las posiciones que
    // da la librería son las de `vehicles` tal cual.
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        onMove(from.index, to.index)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val moveUp = stringResource(Res.string.vehicles_move_up)
    val moveDown = stringResource(Res.string.vehicles_move_down)

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(vehicles, key = { it.id }) { vehicle ->
            ReorderableItem(reorderState, key = vehicle.id, enabled = isReordering) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp)
                val index = vehicles.indexOf(vehicle)

                // Para el lector de pantalla, que no arrastra: subir y bajar de uno en uno.
                val accessibility = if (isReordering) {
                    Modifier.semantics {
                        customActions = listOfNotNull(
                            CustomAccessibilityAction(moveUp) { onMove(index, index - 1); true }
                                .takeIf { index > 0 },
                            CustomAccessibilityAction(moveDown) { onMove(index, index + 1); true }
                                .takeIf { index < vehicles.lastIndex }
                        )
                    }
                } else {
                    Modifier
                }

                Surface(shadowElevation = elevation, modifier = accessibility) {
                    Column {
                        VehicleRow(
                            vehicle = vehicle,
                            currentUserId = currentUserId,
                            onClick = if (isReordering) null else ({ onOpen(vehicle.id) }),
                            trailing = {
                                if (isReordering) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        // Decorativa: el lector de pantalla tiene «Subir» y «Bajar».
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .draggableHandle(
                                                onDragStarted = {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            )
                                            .padding(8.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleRow(
    vehicle: Vehicle,
    currentUserId: String?,
    /** `null` mientras se ordena: la fila no se puede tocar, para que tocar y arrastrar no se pisen. */
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // El tipo y el color, como en la tarjeta del mapa (spec 015). Sin ninguno, nada.
        if (vehicle.hasAvatar) {
            VehicleAvatar(vehicle, size = 40.dp)
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = vehicle.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ParkedText(
                location = vehicle.lastLocation,
                currentUserId = currentUserId,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (vehicle.licensePlate.isNotBlank()) {
                Text(
                    text = vehicle.licensePlate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
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
