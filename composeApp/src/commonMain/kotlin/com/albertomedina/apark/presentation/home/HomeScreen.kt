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
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.presentation.components.AparKMap
import com.albertomedina.apark.presentation.components.AparkBottomNavigationBar
import com.albertomedina.apark.presentation.components.DynamicTimeText
import com.albertomedina.apark.utils.SnackbarMessage
import org.jetbrains.compose.resources.stringResource
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

    // Traducción dinámica de mensajes del ViewModel
    val translatedText = when (state.snackbarMessage?.message) {
        "success_location_updated" -> stringResource(Res.string.success_location_updated)
        "error_location_save" -> stringResource(Res.string.error_location_save)
        "error_gps_permissions" -> stringResource(Res.string.error_gps_permissions)
        "error_undo_failed" -> stringResource(Res.string.error_undo_failed)
        else -> state.snackbarMessage?.message
    }

    val undoLabel = stringResource(Res.string.undo)

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            activeSnackbarMessage = msg
            
            // Si hay datos para deshacer, mostramos el Snackbar con acción
            val result = if (state.locationUpdateSuccessData != null) {
                snackbarHostState.showSnackbar(
                    message = translatedText ?: msg.message,
                    actionLabel = undoLabel,
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )
            } else {
                snackbarHostState.showSnackbar(
                    message = translatedText ?: msg.message,
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )
                SnackbarResult.Dismissed
            }

            if (result == SnackbarResult.ActionPerformed) {
                state.locationUpdateSuccessData?.let { undoData ->
                    viewModel.onEvent(
                        HomeEvent.UndoLocationClicked(
                            undoData.vehicleId,
                            undoData.previousLocation
                        )
                    )
                }
            }
            viewModel.onEvent(HomeEvent.SnackBarDismissed)
        }
    }
    
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { state.vehicles.size + 1 }
    )

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage <= state.vehicles.size) {
            viewModel.onEvent(HomeEvent.OnVehicleSwiped(pagerState.currentPage))
        }
    }

    LaunchedEffect(state.shouldNavigateToLogin) {
        if (state.shouldNavigateToLogin) {
            onNavigateToLogin()
            viewModel.onEvent(HomeEvent.NavigationHandled)
        }
    }

    Scaffold(
        bottomBar = { AparkBottomNavigationBar() },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {

            AparKMap(
                Modifier.fillMaxSize(),
                pagerHeight,
                state.vehicles,
                state.selectedVehicleIndex,
                state.centerCameraTrigger,
                onMarkerDragged = {id, latitude, longitude ->
                    viewModel.onEvent(HomeEvent.OnMarkerDragged(id, latitude, longitude))

                }
            )

            FloatingActionButton(
                onClick = { viewModel.onEvent(HomeEvent.CenterMapOnUserClicked) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = pagerHeight + 8.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = stringResource(Res.string.home_center_map)
                )
            }

            SnackbarHost(
                snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = paddingValues.calculateTopPadding())
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
                    .padding(bottom = paddingValues.calculateBottomPadding() + 24.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    pageSpacing = 16.dp
                ) { page ->
                    if (page == state.vehicles.size) {
                        AddVehicleCard(onClick = onNavigateToAddVehicle)
                    } else {
                        val vehicle = state.vehicles[page]

                        VehicleCard(
                            isLoading = state.isLoading,
                            isSpecificLoading = state.updatingVehicleId == vehicle.id,
                            vehicle = vehicle,
                            onClick = { onNavigateToDetails(vehicle.id) },
                            onUpdateLocation = { viewModel.onEvent(HomeEvent.UpdateLocationClicked(vehicle.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    isLoading: Boolean,           // Estado global para deshabilitar
    isSpecificLoading: Boolean,   // Estado local para mostrar el spinner    vehicle: Vehicle,
    onClick: () -> Unit,
    onUpdateLocation: () -> Unit
    ) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = vehicle.name, style = MaterialTheme.typography.titleLarge)

                val timestamp = vehicle.lastLocation?.timestamp

                val lastLocation = vehicle.lastLocation
                val userName = lastLocation?.user?.name?.takeIf { it.isNotBlank() }
                val userEmail = lastLocation?.user?.email?.takeIf { it.isNotBlank() }

                val displayName = userName ?: userEmail ?: ""
                val updatedBy = stringResource(Res.string.home_parked_by, displayName)

                DynamicTimeText(
                    timestamp = timestamp,
                    text = Res.string.home_last_time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = updatedBy,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isLoading && isSpecificLoading){
                CircularProgressIndicator()
            }else{
                Button(
                    onClick = onUpdateLocation,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text(stringResource(Res.string.home_park_here))
                }
            }


        }
    }
}

@Composable
fun AddVehicleCard(onClick: () -> Unit) {
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
                contentDescription = stringResource(Res.string.home_add_vehicle),
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.home_add_vehicle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
