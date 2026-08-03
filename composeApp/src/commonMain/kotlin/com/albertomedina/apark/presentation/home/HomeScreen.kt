package com.albertomedina.apark.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.presentation.components.AparKConfirmDialog
import com.albertomedina.apark.presentation.components.AparKMap
import com.albertomedina.apark.presentation.components.AparkBottomNavigationBar
import com.albertomedina.apark.presentation.components.DynamicTimeText
import com.albertomedina.apark.presentation.components.LocationPermissionHandler
import com.albertomedina.apark.utils.OpenAppSettingsHandler
import com.albertomedina.apark.utils.SnackbarMessage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
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

    var showMenu by remember { mutableStateOf(false) }

    var hasLocationPermission by remember { mutableStateOf(false) }

    OpenAppSettingsHandler(
        trigger = state.openSettingsTrigger,
        onSettingsOpened = {  }
    )

    LocationPermissionHandler(
        onPermissionGranted = {
            hasLocationPermission = true
        },
        onPermissionDenied = {
            hasLocationPermission = false
            // Opcional: Aquí podrías disparar un evento a tu ViewModel
            // para mostrar un Snackbar que diga: "Necesitamos tu ubicación para aparcar"
        }
    )

    // Traducción dinámica de mensajes del ViewModel
    val translatedText = when (state.snackbarMessage?.message) {
        "success_location_updated" -> stringResource(Res.string.success_location_updated)
        "error_location_save" -> stringResource(Res.string.error_location_save)
        "error_gps_permissions" -> stringResource(Res.string.error_gps_permissions)
        "error_undo_failed" -> stringResource(Res.string.error_undo_failed)
        HomeViewModel.SUCCESS_DELETED_KEY -> stringResource(Res.string.delete_vehicle_success_deleted)
        HomeViewModel.SUCCESS_REMOVED_KEY -> stringResource(Res.string.delete_vehicle_success_removed)
        HomeViewModel.ERROR_DELETE_KEY -> stringResource(Res.string.delete_vehicle_error)
        HomeViewModel.ERROR_NOT_AUTHENTICATED_KEY -> stringResource(Res.string.delete_vehicle_error_not_authenticated)
        HomeViewModel.ERROR_REORDER_KEY -> stringResource(Res.string.reorder_error)
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
                    withDismissAction = false,
                    duration = SnackbarDuration.Long
                )
            }else if (msg.message == "error_gps_permissions") {
                snackbarHostState.showSnackbar(
                    message = "Permisos de GPS denegados. Ve a ajustes para activarlos.",
                    actionLabel = "Ajustes",
                    withDismissAction = false,
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

                if (msg.message == "error_gps_permissions"){
                    viewModel.onEvent(HomeEvent.OpenSettingsClicked)
                }else{
                    state.locationUpdateSuccessData?.let { undoData ->
                        viewModel.onEvent(
                            HomeEvent.UndoLocationClicked(
                                undoData.vehicleId,
                                undoData.previousLocation
                            )
                        )
                    }

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

    // Keep the vehicle being reordered centred, so its arrows stay under the user's finger.
    // Re-runs on every list update rather than once: the reordered list may arrive after this
    // would first run, and moving twice quickly would otherwise leave the pager one short.
    LaunchedEffect(state.followedVehicleId, state.vehicles) {
        val vehicleId = state.followedVehicleId ?: return@LaunchedEffect
        val target = state.vehicles.indexOfFirst { it.id == vehicleId }

        if (target != -1 && pagerState.currentPage != target) {
            // Instant, not animated: a following tap would interrupt an animated scroll and
            // strand the pager a position behind. It also reads better — the card being moved
            // appears to stay put while its neighbours swap around it.
            pagerState.scrollToPage(target)
        }
    }

    // Deleting shrinks the list: pull the current page back into range, otherwise the pager
    // keeps pointing past the end (and the map would render a stale selection).
    LaunchedEffect(state.vehicles.size) {
        val lastPage = state.vehicles.size
        if (pagerState.currentPage > lastPage) {
            pagerState.scrollToPage(lastPage)
            viewModel.onEvent(HomeEvent.OnVehicleSwiped(lastPage))
        }
    }

    LaunchedEffect(state.shouldNavigateToLogin) {
        if (state.shouldNavigateToLogin) {
            onNavigateToLogin()
            viewModel.onEvent(HomeEvent.NavigationHandled)
        }
    }

    // While editing, back leaves the mode rather than the screen. Only enabled in edit mode,
    // so normal back behaviour is untouched.
    BackHandler(enabled = state.isEditMode) {
        viewModel.onEvent(HomeEvent.EditModeExited)
    }

    Scaffold(
        bottomBar = { AparkBottomNavigationBar() },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {

            AparKMap(
                Modifier.fillMaxSize(),
                pagerHeight,
                state.vehicles,
                state.selectedVehicleIndex.coerceIn(0, maxOf(0, state.vehicles.lastIndex)),
                state.centerCameraTrigger,
                onMarkerDragged = {id, latitude, longitude ->
                    viewModel.onEvent(HomeEvent.OnMarkerDragged(id, latitude, longitude))

                }
            )

            // While editing, a scrim locks the map: it swallows gestures that would otherwise
            // pan it or drag a marker, dims it to signal the mode, and doubles as the way out.
            if (state.isEditMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.onEvent(HomeEvent.EditModeExited) }
                )
            }

            FloatingActionButton(
                onClick = {
                    if (hasLocationPermission){
                        viewModel.onEvent(HomeEvent.CenterMapOnUserClicked)
                    }else{
                        viewModel.onEvent(HomeEvent.PermisionsDenied)
                    }
                          },
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

            FloatingActionButton(
                onClick = {
                    showMenu = true
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = paddingValues.calculateTopPadding() + 16.dp, end = 16.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = ""
                )

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Logout") }, // Asegúrate de tener este string
                        leadingIcon = {
                            Icon(Icons.Default.Logout, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            viewModel.onEvent(HomeEvent.SignOutClicked)
                        }
                    )
                }
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
                Column(modifier = Modifier.fillMaxWidth()) {

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        pageSpacing = 16.dp
                    ) { page ->
                        // getOrNull: while the list shrinks after a delete, the pager can
                        // briefly render a page index that no longer exists.
                        val vehicle = state.vehicles.getOrNull(page)

                        if (vehicle == null) {
                            AddVehicleCard(onClick = onNavigateToAddVehicle)
                        } else {
                            VehicleCard(
                                isLoading = state.isLoading,
                                isSpecificLoading = state.updatingVehicleId == vehicle.id,
                                vehicle = vehicle,
                                isEditMode = state.isEditMode,
                                isOwner = vehicle.ownerId == state.currentUserId,
                                canMoveLeft = page > 0,
                                canMoveRight = page < state.vehicles.lastIndex,
                                onClick = { onNavigateToDetails(vehicle.id) },
                                onLongClick = { viewModel.onEvent(HomeEvent.VehicleLongPressed) },
                                onDelete = { viewModel.onEvent(HomeEvent.DeleteVehicleClicked(vehicle.id)) },
                                onMoveLeft = { viewModel.onEvent(HomeEvent.MoveVehicleClicked(vehicle.id, -1)) },
                                onMoveRight = { viewModel.onEvent(HomeEvent.MoveVehicleClicked(vehicle.id, 1)) },
                                onUpdateLocation = {
                                    if (hasLocationPermission){
                                        viewModel.onEvent(HomeEvent.UpdateLocationClicked(vehicle.id))
                                    }else{
                                        viewModel.onEvent(HomeEvent.PermisionsDenied)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            state.pendingDeletion?.let { pending ->
                AparKConfirmDialog(
                    title = if (pending.isOwner) {
                        stringResource(Res.string.delete_vehicle_confirm_title, pending.vehicleName)
                    } else {
                        stringResource(Res.string.remove_vehicle_confirm_title, pending.vehicleName)
                    },
                    text = if (pending.isOwner) {
                        stringResource(Res.string.delete_vehicle_confirm_message)
                    } else {
                        stringResource(Res.string.remove_vehicle_confirm_message)
                    },
                    confirmLabel = if (pending.isOwner) {
                        stringResource(Res.string.delete_vehicle_confirm_action)
                    } else {
                        stringResource(Res.string.remove_vehicle_confirm_action)
                    },
                    dismissLabel = stringResource(Res.string.cancel),
                    isDestructive = pending.isOwner,
                    onConfirm = { viewModel.onEvent(HomeEvent.DeleteConfirmed) },
                    onDismiss = { viewModel.onEvent(HomeEvent.DeleteDismissed) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VehicleCard(
    vehicle: Vehicle,
    isLoading: Boolean,           // Estado global para deshabilitar
    isSpecificLoading: Boolean,   // Estado local para mostrar el spinner    vehicle: Vehicle,
    isEditMode: Boolean,
    isOwner: Boolean,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onUpdateLocation: () -> Unit
    ) {
    Box {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = vehicle.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge
                )


                val lastLocation = vehicle.lastLocation
                val hasLocation = lastLocation != null && lastLocation.timestamp != 0L

                if (!hasLocation) {
                    Text(
                        text = stringResource(Res.string.home_not_parked_yet),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val userName = lastLocation.user?.name?.takeIf { it.isNotBlank() }
                    val userEmail = lastLocation.user?.email?.takeIf { it.isNotBlank() }

                    val displayName = userName ?: userEmail ?: ""
                    val updatedBy = stringResource(Res.string.home_parked_by, displayName)

                    DynamicTimeText(
                        timestamp = lastLocation.timestamp,
                        text = Res.string.home_last_time,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = updatedBy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isLoading && isSpecificLoading){
                CircularProgressIndicator()
            } else if (isEditMode) {
                // Editing reuses the parking button's slot for the reorder arrows: it is
                // disabled here anyway, so nothing is covered and the height stays put.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onMoveLeft, enabled = canMoveLeft) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = stringResource(Res.string.reorder_move_left)
                        )
                    }
                    IconButton(onClick = onMoveRight, enabled = canMoveRight) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = stringResource(Res.string.reorder_move_right)
                        )
                    }
                }
            } else {
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

        if (isEditMode) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clickable { onDelete() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isOwner) Icons.Default.Delete else Icons.Default.Close,
                        contentDescription = if (isOwner) {
                            stringResource(Res.string.delete_vehicle_action)
                        } else {
                            stringResource(Res.string.remove_vehicle_action)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddVehicleCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.home_add_vehicle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
