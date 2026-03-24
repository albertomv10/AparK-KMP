package com.albertomedina.apark.presentation.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.presentation.components.AparKMap
import com.albertomedina.apark.utils.SnackbarMessage
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    onNavigateToAddVehicle: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeSnackbarMessage by remember { mutableStateOf<SnackbarMessage?>(null) }
    var pagerHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    LaunchedEffect(state.snackbarMessage) {

        state.locationUpdateSuccessData?.let { undoData ->
            state.snackbarMessage?.let { msg ->
                activeSnackbarMessage = msg
            val result = snackbarHostState.showSnackbar(
                message = msg.message ,
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Long
            )

            when (result) {
                SnackbarResult.ActionPerformed -> {
                    viewModel.onEvent(
                        HomeEvent.UndoLocationClicked(
                            undoData.vehicleId,
                            undoData.previousLocation
                        )
                    )
                }
                SnackbarResult.Dismissed -> {
                    // Se cerró solo (pasó el tiempo) o lo deslizó. No hacemos nada especial.
                }
            }

            viewModel.onEvent(HomeEvent.SnackBarDismissed)

            }
        }
    }
    
    // 1. El carrusel ahora tiene el tamaño de vehículos + 1 (la tarjeta de Añadir)
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { state.vehicles.size + 1 }
    )

    // 2. Control de deslizamiento (Solo enfocamos el mapa si NO es la tarjeta de Añadir)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < state.vehicles.size) {
            viewModel.onEvent(HomeEvent.OnVehicleSwiped(pagerState.currentPage))
        }
    }

    // Navegación de logout
    LaunchedEffect(state.shouldNavigateToLogin) {
        if (state.shouldNavigateToLogin) {
            onNavigateToLogin()
            viewModel.onEvent(HomeEvent.NavigationHandled)
        }
    }

    // Quitamos el FAB del Scaffold porque ahora la tarjeta tiene el "+"
    Scaffold (
        snackbarHost = {

        }
    ) { paddingValues ->

        // BOX es la clave: Permite superponer elementos
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // ==========================================
            // CAPA 1: EL MAPA (Ocupa toda la pantalla)
            // ==========================================
                AparKMap(
                    Modifier.fillMaxSize(),
                    pagerHeight,
                    state.vehicles,
                    state.selectedVehicleIndex,
                    state.centerCameraTrigger
                )

            FloatingActionButton(
                onClick = { viewModel.onEvent(HomeEvent.CenterMapOnUserClicked) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = paddingValues.calculateTopPadding(), end = 16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Centrar en mi ubicación"
                )
            }

            SnackbarHost(
                snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter) // 👈 ¡LA MAGIA! Lo anclamos arriba al centro
                    .padding(top = paddingValues.calculateTopPadding())       // Le damos un poco de aire para que no se pegue al "Notch" o borde del móvil
                    .zIndex(1f)
            ) { snackbarData ->
                activeSnackbarMessage?.let { customMsg ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = customMsg.backgroundColor(),
                        contentColor = customMsg.contentColor(),
                        actionColor = customMsg.contentColor()
                    )
                } ?: Snackbar(snackbarData = snackbarData)
            }


            // ==========================================
            // CAPA 2: EL CARRUSEL FLOTANTE
            // ==========================================
            // TRUCO PRO: Añadimos un degradado negro abajo para que las tarjetas
            // siempre se lean bien aunque el mapa debajo sea blanco
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                        )
                    )
                    .onGloballyPositioned{ coordinates ->
                        pagerHeight = with(density){coordinates.size.height.toDp() }
                    }
                    .padding(bottom = 108.dp) // Espacio para que respire
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 48.dp), // Tarjetas más asomadas
                    pageSpacing = 16.dp
                ) { page ->

                    // LÓGICA DE TARJETAS
                    if (page == state.vehicles.size) {
                        // Si es la última página, pintamos la tarjeta de Añadir
                        AddVehicleCard(onClick = onNavigateToAddVehicle)
                    } else {
                        // Si es una página normal, pintamos el vehículo
                        val vehicle = state.vehicles[page]
                        VehicleCard(
                            vehicle = vehicle,
                            onClick = { onNavigateToDetails(vehicle.id) },
                            onUpdateLocation = { viewModel.onEvent(HomeEvent.UpdateLocationClicked(vehicle.id)) }
                        )
                    }
                }
            }

            // Botón de Logout temporal arriba a la derecha (para no perder la funcionalidad)
//            IconButton(
//                onClick = { viewModel.onEvent(HomeEvent.SignOutClicked) },
//                modifier = Modifier
//                    .align(Alignment.TopEnd)
//                    .padding(16.dp)
//                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(50))
//            ) {
//                Text("Salir", modifier = Modifier.padding(4.dp))
//            }
        }
    }
}

// --- Componentes Visuales ---

@Composable
@Preview
fun VehicleCard(vehicle: Vehicle, onClick: () -> Unit, onUpdateLocation: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp), // Altura fija para que todas las tarjetas sean iguales
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = vehicle.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Última vez: Hace 2 horas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onUpdateLocation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Aparcar aquí")
            }
        }
    }
}

@Composable
fun AddVehicleCard(onClick: () -> Unit) {
    // Tarjeta de diseño distinto (borde punteado o color sutil)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Añadir Vehículo",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Añadir nuevo vehículo",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}