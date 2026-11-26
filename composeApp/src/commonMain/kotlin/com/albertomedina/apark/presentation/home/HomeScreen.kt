package com.albertomedina.apark.presentation.home

import com.albertomedina.apark.presentation.vehicleform.VehicleAvatar
import com.albertomedina.apark.presentation.navigation.TabShellDefaults
import com.albertomedina.apark.presentation.vehicleform.hasAvatar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import androidx.compose.ui.zIndex
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.VehicleType
import com.albertomedina.apark.presentation.components.AparKMap
import com.albertomedina.apark.presentation.components.LocationPermissionHandler
import com.albertomedina.apark.presentation.components.ParkedText
import com.albertomedina.apark.presentation.components.mapLocation
import com.albertomedina.apark.presentation.incidents.IncidentTypeIcon
import com.albertomedina.apark.presentation.incidents.cardIncidents
import com.albertomedina.apark.presentation.incidents.displayName
import com.albertomedina.apark.presentation.vehicleform.icon
import com.albertomedina.apark.utils.OpenAppSettingsHandler
import com.albertomedina.apark.utils.SnackbarMessage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToDetails: (String) -> Unit,
    onNavigateToAddVehicle: () -> Unit,
    /** «Ver en el mapa» desde el detalle (spec 014): el vehículo que hay que poner delante. */
    focusVehicleId: String? = null,
    onFocusHandled: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeSnackbarMessage by remember { mutableStateOf<SnackbarMessage?>(null) }
    var pagerHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

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
        "error_vehicles_load" -> stringResource(Res.string.vehicles_load_error)
        else -> state.snackbarMessage?.message
    }

    val undoLabel = stringResource(Res.string.undo)
    val settingsLabel = stringResource(Res.string.open_settings)

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            activeSnackbarMessage = msg
            
            // El permiso va primero: si llega mientras sigue en pantalla el aviso de «aparcado»,
            // los datos para deshacer todavía están puestos, y con el orden al revés el aviso de
            // GPS salía con el botón «Deshacer»... que abría los ajustes.
            val result = if (msg.message == "error_gps_permissions") {
                snackbarHostState.showSnackbar(
                    message = translatedText ?: msg.message,
                    actionLabel = settingsLabel,
                    withDismissAction = false,
                    duration = SnackbarDuration.Long
                )
            } else if (state.locationUpdateSuccessData != null) {
                // Si hay datos para deshacer, mostramos el Snackbar con acción
                snackbarHostState.showSnackbar(
                    message = translatedText ?: msg.message,
                    actionLabel = undoLabel,
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

    // La selección es del ViewModel, y el pager la sigue; lo único que el pager le cuenta son los
    // cambios de página. La página con la que nace no: al volver a esta pestaña el pager recupera la
    // que tenía guardada, que puede ser vieja —si entretanto se ha ordenado desde *Vehículos*—, y
    // comunicarla pisaría la selección que el ViewModel ya había movido con su vehículo (spec 016).
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .drop(1)
            .collect { page -> viewModel.onEvent(HomeEvent.OnVehicleSwiped(page)) }
    }

    // El carrusel sigue al vehículo que enseñaba cuando la lista cambia de orden (spec 016): el
    // ViewModel mueve la selección con él, y aquí el pager va detrás. Sin animar, para que parezca que
    // la tarjeta no se ha movido. Al deslizar, la selección ya es la página actual y esto no hace nada.
    LaunchedEffect(state.selectedVehicleIndex) {
        val target = state.selectedVehicleIndex
        if (target in 0..state.vehicles.size && pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    // «Ver en el mapa» (spec 014): el carrusel va a ese vehículo, y el mapa lo sigue, porque se
    // centra en el seleccionado. Se espera a que llegue la lista; si llega y el vehículo no está
    // —lo han borrado entretanto—, se da por atendido para no dejarlo pendiente para siempre.
    LaunchedEffect(focusVehicleId, state.vehicles) {
        val vehicleId = focusVehicleId ?: return@LaunchedEffect
        val target = state.vehicles.indexOfFirst { it.id == vehicleId }
        if (target == -1) {
            if (state.vehicles.isNotEmpty()) onFocusHandled()
            return@LaunchedEffect
        }
        pagerState.scrollToPage(target)
        onFocusHandled()
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

    // El mapa acaba de aparecer, al arrancar o al volver a esta pestaña: lo siguiente es un salto, no
    // un vuelo (spec 017). Va antes que el mapa, para que la orden esté puesta cuando empiece a
    // escucharlas.
    LaunchedEffect(Unit) {
        viewModel.onEvent(HomeEvent.MapShown)
    }

    val haptics = LocalHapticFeedback.current
    val selectedVehicle = state.vehicles.getOrNull(state.selectedVehicleIndex)

    Scaffold(contentWindowInsets = TabShellDefaults.contentWindowInsets) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {

            AparKMap(
                modifier = Modifier.fillMaxSize(),
                bottomPadding = pagerHeight,
                vehicles = state.vehicles,
                selectedVehicleIndex = state.selectedVehicleIndex.coerceIn(0, maxOf(0, state.vehicles.lastIndex)),
                cameraRequests = viewModel.cameraRequests,
                onMarkerDragged = { id, latitude, longitude ->
                    viewModel.onEvent(HomeEvent.ManualLocationChosen(id, latitude, longitude))
                },
                onMarkerTapped = { viewModel.onEvent(HomeEvent.MarkerTapped(it)) },
                onMapLongPressed = { latitude, longitude ->
                    // Con la tarjeta «+» no hay vehículo que mover: ni vibra.
                    if (selectedVehicle != null) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.onEvent(HomeEvent.MapLongPressed(latitude, longitude))
                    }
                }
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = pagerHeight + 8.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Volver al vehículo elegido, de cerca (spec 017). Con su icono, para que no se
                // confunda con el de centrar en ti, y del mismo tamaño: los dos se usan con una mano,
                // a menudo en la calle. Sin aparcar, no hay adónde volver.
                if (selectedVehicle?.mapLocation != null) {
                    FloatingActionButton(
                        onClick = { viewModel.onEvent(HomeEvent.GoToVehicleClicked) },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = VehicleType.fromCode(selectedVehicle.type).icon,
                            contentDescription = stringResource(Res.string.map_go_to_vehicle)
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        if (hasLocationPermission) {
                            viewModel.onEvent(HomeEvent.CenterMapOnUserClicked)
                        } else {
                            viewModel.onEvent(HomeEvent.PermisionsDenied)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = stringResource(Res.string.home_center_map)
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
                                vehicle = vehicle,
                                incidents = state.openIncidentsOf(vehicle.id),
                                currentUserId = state.currentUserId,
                                isLoading = state.isLoading,
                                isSpecificLoading = state.updatingVehicleId == vehicle.id,
                                onClick = { onNavigateToDetails(vehicle.id) },
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

        }
    }
}

/**
 * El alto mínimo de las tarjetas del carrusel (spec 016). Las de vehículo llegan a él por su propio
 * contenido, que es igual en todas —la fila del nombre, una línea y el botón—, así que todas miden lo
 * mismo y el carrusel no cambia de alto al deslizar. La de «+» lo usa para igualarse con ellas. No es
 * una altura fija: con la letra grande, las tarjetas de vehículo crecen todas a la vez.
 */
private val CardMinHeight = 148.dp

/**
 * Lo que hace falta para el día a día, y nada más (spec 016): qué vehículo es, dónde se dejó y quién,
 * y aparcarlo. Borrar y ordenar viven en el detalle y en *Vehículos*.
 */
@Composable
fun VehicleCard(
    vehicle: Vehicle,
    /** Sus incidencias abiertas (spec 020): un icono por tipo, entre el nombre y la flecha. */
    incidents: List<Incident>,
    currentUserId: String?,
    /** Hay una operación en marcha, de este vehículo o de otro: aparcar queda deshabilitado. */
    isLoading: Boolean,
    /** La operación en marcha es la de este vehículo: el botón enseña que está trabajando. */
    isSpecificLoading: Boolean,
    onClick: () -> Unit,
    onUpdateLocation: () -> Unit
) {
    val shape = CardDefaults.shape
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CardMinHeight)
            .clip(shape)
            // La etiqueta es lo que anuncia el lector de pantalla al llegar a la tarjeta.
            .clickable(onClickLabel = stringResource(Res.string.vehicle_card_open_details), onClick = onClick),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // El nombre y la flecha que dice que la tarjeta abre el detalle, y entre los dos los iconos
            // de lo pendiente (spec 020): el nombre se corta antes que ellos. Mide al menos lo que el
            // avatar, para que con avatar y sin él —y con iconos y sin ellos— las tarjetas midan igual.
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (vehicle.hasAvatar) {
                    VehicleAvatar(vehicle, size = 32.dp)
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text = vehicle.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (incidents.isNotEmpty()) {
                    IncidentIcons(incidents, Modifier.padding(start = 8.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ParkedText(
                location = vehicle.lastLocation,
                currentUserId = currentUserId,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Mientras aparca, el indicador va dentro del botón: cambiar el botón por un indicador
            // suelto cambiaba el alto de la tarjeta.
            Button(
                onClick = onUpdateLocation,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading && isSpecificLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.home_park_here))
                }
            }
        }
    }
}

/**
 * Lo que tiene pendiente un vehículo, en su tarjeta (spec 020): un icono por tipo, como mucho tres, y
 * «+N» con los tipos que no caben. Iconos y no un número, porque «⛽» dice qué le pasa sin abrir nada.
 * Para el lector de pantalla es una sola frase: «Poco combustible, Pinchazo y 2 más».
 */
@Composable
private fun IncidentIcons(incidents: List<Incident>, modifier: Modifier = Modifier) {
    val card = cardIncidents(incidents)
    val names = card.shown.map { it.displayName() }.joinToString(", ")
    val description = if (card.more > 0) stringResource(Res.string.incidents_card_more, names, card.more) else names
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        card.shown.forEach { IncidentTypeIcon(it.kind, size = 24.dp, iconSize = 16.dp) }
        if (card.more > 0) {
            Text(
                "+${card.more}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

@Composable
fun AddVehicleCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CardMinHeight),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(min = CardMinHeight).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
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
