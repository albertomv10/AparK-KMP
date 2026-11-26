package com.albertomedina.apark.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.model.OpenIncidents
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.GetVehicleListUseCase
import com.albertomedina.apark.domain.usecase.LocateUserUseCase
import com.albertomedina.apark.domain.usecase.ObserveOpenIncidentsUseCase
import com.albertomedina.apark.domain.usecase.UpdateVehicleLocationUseCase
import com.albertomedina.apark.presentation.components.CLOSE_ZOOM
import com.albertomedina.apark.presentation.components.CameraMove
import com.albertomedina.apark.presentation.components.CameraRequest
import com.albertomedina.apark.presentation.components.GeoPoint
import com.albertomedina.apark.presentation.components.MapFocus
import com.albertomedina.apark.presentation.components.STREET_ZOOM
import com.albertomedina.apark.presentation.components.cameraMoveFor
import com.albertomedina.apark.presentation.components.mapLocation
import com.albertomedina.apark.utils.SnackbarMessage
import com.albertomedina.apark.utils.catchListenerFailure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val updateVehicleLocationUseCase: UpdateVehicleLocationUseCase,
    private val getVehicleListUseCase: GetVehicleListUseCase,
    private val locateUserUseCase: LocateUserUseCase,
    private val observeOpenIncidents: ObserveOpenIncidentsUseCase,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * Las órdenes para la cámara del mapa (spec 017). *Conflated*: si llegan con la pestaña oculta,
     * se queda solo la última, y el salto de [HomeEvent.MapShown] la sustituye al volver; nunca se
     * reproduce una vieja.
     */
    private val cameraChannel = Channel<CameraRequest>(Channel.CONFLATED)
    val cameraRequests: Flow<CameraRequest> = cameraChannel.receiveAsFlow()

    /** Lo último que enseñó el mapa. `null`: acaba de aparecer, y lo que toca es un salto. */
    private var lastFocus: MapFocus? = null

    /** Averiguar dónde estás puede tardar: si entretanto llega otro movimiento, este sobra. */
    private var locateJob: Job? = null

    init {
        observeAuthState()
        observeIncidents()
    }

    /**
     * Las incidencias abiertas de cada vehículo, para los iconos de su tarjeta (spec 020). Igual que en
     * *Vehículos*: las escuchas se reabren solo cuando cambia **el conjunto** de vehículos, no cada vez
     * que alguien aparca, que es lo que más cambia aquí.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeIncidents() {
        viewModelScope.launch {
            _uiState
                .map { state -> state.vehicles.mapTo(mutableSetOf(), Vehicle::id) }
                .distinctUntilChanged()
                .flatMapLatest { ids ->
                    observeOpenIncidents(ids, { authRepository.getCurrentUser() != null }) { error ->
                        crashReporter.recordError(error, INCIDENTS_LISTENER_CONTEXT)
                    }
                }
                .collect { open -> _uiState.update { it.copy(openIncidents = it.openIncidents + open) } }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authStateChanges
                .map { it?.uid }
                // `authStateChanges` también emite al refrescarse el token —cada hora, más o
                // menos—, no solo al entrar o salir. Sin este filtro, cada refresco cancelaba los
                // listeners de Firestore y los volvía a abrir para el mismo usuario: dos
                // suscripciones reiniciadas y una relectura completa a cambio de nada.
                .distinctUntilChanged()
                .flatMapLatest { userId ->
                    // Keep the uid around: the UI needs it to tell owner from shared member. Lo que se
                    // supiera de las incidencias era del usuario anterior.
                    _uiState.update { it.copy(currentUserId = userId, openIncidents = emptyMap()) }
                    // Ata los informes de fallo a un usuario — el uid, nunca el email — y los
                    // desata al cerrar sesión. Va aquí porque es donde el estado de auth ya fluye.
                    crashReporter.setUserId(userId)
                    if (!userId.isNullOrBlank()) {
                        // Si la escucha falla, se queda la lista que hubiera y se avisa (spec 021).
                        getVehicleListUseCase(userId)
                            .catchListenerFailure({ authRepository.getCurrentUser() != null }) { error ->
                                crashReporter.recordError(error, VEHICLES_LISTENER_CONTEXT)
                                _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_vehicles_load")) }
                            }
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { vehicleList ->
                    _uiState.update { it.withVehicles(vehicleList).copy(vehiclesLoaded = true) }
                    updateCamera()
                }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnVehicleSwiped -> {
                _uiState.update {
                    it.copy(
                        selectedVehicleIndex = event.newIndex,
                        selectedVehicleId = it.vehicles.getOrNull(event.newIndex)?.id
                    )
                }
                updateCamera()
            }

            // Tocar un marcador elige su vehículo: el carrusel va a él por el efecto de selección de
            // la pantalla, y la cámara por la regla de siempre.
            is HomeEvent.MarkerTapped -> {
                val index = _uiState.value.vehicles.indexOfFirst { it.id == event.vehicleId }
                if (index >= 0) {
                    _uiState.update { it.copy(selectedVehicleIndex = index, selectedVehicleId = event.vehicleId) }
                    updateCamera()
                }
            }

            // Mantener pulsado el mapa mueve ahí el vehículo elegido, igual que arrastrar su
            // marcador. Con la tarjeta «+» no hay vehículo que mover.
            is HomeEvent.MapLongPressed -> {
                val state = _uiState.value
                state.vehicles.getOrNull(state.selectedVehicleIndex)?.let {
                    saveManualLocation(it.id, event.latitude, event.longitude)
                }
            }

            is HomeEvent.MapShown -> {
                lastFocus = null
                updateCamera()
            }

            is HomeEvent.GoToVehicleClicked -> {
                val state = _uiState.value
                // De cerca: elegir el vehículo ya lo enseña a zoom de calle, y este botón sirve para
                // ver exactamente dónde está.
                state.vehicles.getOrNull(state.selectedVehicleIndex)?.mapLocation?.let {
                    perform(CameraMove.ToPoint(it, CLOSE_ZOOM, animated = true))
                }
            }

            is HomeEvent.UpdateLocationClicked -> {
                updateVehicleLocation(event.vehicleId)
            }

            is HomeEvent.UndoLocationClicked -> {
                undoVehicleLocation(event.vehicleId, event.previousLocation)
            }

            is HomeEvent.ManualLocationChosen -> {
                saveManualLocation(event.vehicleId, event.latitude, event.longitude)
            }

            is HomeEvent.CenterMapOnUserClicked -> perform(CameraMove.ToUser(animated = true))

            is HomeEvent.SnackBarDismissed -> {
                _uiState.update {
                    it.copy(
                        locationUpdateSuccessData = null,
                        snackbarMessage = null
                    )
                }
            }
            is HomeEvent.OpenSettingsClicked -> {
                _uiState.update { it.copy(openSettingsTrigger = it.openSettingsTrigger + 1) }
            }

            is HomeEvent.PermisionsDenied -> {
                _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_gps_permissions")) }
            }

        }
    }

    private fun updateVehicleLocation(vehicleId: String) {
        val vehicle = _uiState.value.vehicles.find { it.id == vehicleId }
        val previousLocation = vehicle?.lastLocation
        _uiState.update { it.copy(isLoading = true, updatingVehicleId = vehicleId) }

        viewModelScope.launch {
            try {
                val result = updateVehicleLocationUseCase(vehicleId)

                result.fold(
                    onSuccess = { confirmParked(vehicleId, previousLocation) },
                    onFailure = {
                        _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_location_save")) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_gps_permissions")) }
            } finally {
                _uiState.update { it.copy(isLoading = false, updatingVehicleId = null) }
            }
        }
    }

    private fun undoVehicleLocation(vehicleId: String, previousLocation: Vehicle.LocationModel) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = updateVehicleLocationUseCase(vehicleId, previousLocation, true)

            result.fold(
                onSuccess = {

                },
                onFailure = {
                    _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_undo_failed")) }
                }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun saveManualLocation(vehicleId: String, latitude: Double, longitude: Double) {
        _uiState.update { it.copy(isLoading = true, updatingVehicleId = vehicleId) }
        val vehicle = _uiState.value.vehicles.find { it.id == vehicleId }
        val previousLocation = vehicle?.lastLocation
        viewModelScope.launch {
            val result = updateVehicleLocationUseCase(
                vehicleId = vehicleId,
                manualLocation = Vehicle.LocationModel(
                    latitude = latitude,
                    longitude = longitude
                )
            )
            result.fold(
                onSuccess = { confirmParked(vehicleId, previousLocation) },
                onFailure = {
                    _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_location_save")) }
                }
            )

            _uiState.update { it.copy(isLoading = false, updatingVehicleId = null) }
        }
    }

    /**
     * Mueve la cámara si el foco ha cambiado (spec 017). Solo con la lista ya cargada: si no, al
     * arrancar se iría a tu ubicación y después volaría hasta el vehículo.
     */
    private fun updateCamera() {
        val state = _uiState.value
        if (!state.vehiclesLoaded) return
        val selected = state.vehicles.getOrNull(state.selectedVehicleIndex)
        val focus = MapFocus(selected?.id, selected?.mapLocation)
        val move = cameraMoveFor(lastFocus, focus)
        lastFocus = focus
        move?.let(::perform)
    }

    private fun perform(move: CameraMove) {
        locateJob?.cancel()
        when (move) {
            is CameraMove.ToPoint -> cameraChannel.trySend(CameraRequest(move.point, move.zoom, move.animated))
            is CameraMove.ToUser -> locateJob = viewModelScope.launch {
                // Sin ubicación —sin permiso, o sin señal—, la cámara se queda donde está.
                val here = locateUserUseCase() ?: return@launch
                cameraChannel.trySend(CameraRequest(GeoPoint(here.latitude, here.longitude), STREET_ZOOM, move.animated))
            }
        }
    }

    /**
     * Aparcar siempre confirma (spec 016). Antes el aviso solo salía si había una ubicación a la que
     * volver con «Deshacer», así que aparcar por primera vez no daba ninguna señal. Sin ubicación
     * anterior —o con la vacía, `timestamp == 0`— sale igual, sin nada que deshacer.
     */
    private fun confirmParked(vehicleId: String, previousLocation: Vehicle.LocationModel?) {
        val undo = previousLocation?.takeIf { it.timestamp != 0L }?.let { UndoLocationData(vehicleId, it) }
        _uiState.update {
            it.copy(
                locationUpdateSuccessData = undo,
                snackbarMessage = SnackbarMessage.Success("success_location_updated")
            )
        }
    }

}

/**
 * Llega la lista. Si el vehículo que se estaba viendo ha cambiado de posición —porque se ha ordenado
 * desde *Vehículos* (spec 016), o ha entrado o salido otro antes que él—, la selección se va con él:
 * el carrusel sigue enseñando el mismo vehículo en vez del que haya caído en su sitio. Si ya no está,
 * se queda la posición y la pantalla la devuelve a un sitio válido.
 */
private fun HomeUiState.withVehicles(list: List<Vehicle>): HomeUiState {
    val followed = selectedVehicleId?.let { id -> list.indexOfFirst { it.id == id } }?.takeIf { it >= 0 }
    val index = followed ?: selectedVehicleIndex
    return copy(vehicles = list, selectedVehicleIndex = index, selectedVehicleId = list.getOrNull(index)?.id)
}

/** Agrupa en Crashlytics los fallos de la escucha de la lista (spec 021). */
private const val VEHICLES_LISTENER_CONTEXT = "home_vehicles_listener"

/** Y los de las incidencias de la tarjeta (spec 020). */
private const val INCIDENTS_LISTENER_CONTEXT = "home_incidents_listener"

data class HomeUiState(
    val currentUserId: String? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleIndex: Int = 0,
    /** El vehículo que enseña el carrusel, para seguirlo si cambia de posición; `null` en la tarjeta «+». */
    val selectedVehicleId: String? = null,
    /** Ya ha llegado la lista al menos una vez: hasta entonces la cámara no sabe adónde ir. */
    val vehiclesLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val updatingVehicleId: String? = null,
    val openSettingsTrigger: Int = 0,
    val locationUpdateSuccessData: UndoLocationData? = null,
    val snackbarMessage: SnackbarMessage? = null,
    /** Lo que se sabe de las incidencias abiertas de cada vehículo, por su id (spec 020). */
    val openIncidents: Map<String, OpenIncidents> = emptyMap()
) {
    /**
     * Las abiertas de un vehículo, para su tarjeta. Si no se han podido cargar, ninguna: la tarjeta es
     * para el día a día, y el fallo ya lo dice «Pendiente», en *Vehículos*.
     */
    fun openIncidentsOf(vehicleId: String): List<Incident> =
        (openIncidents[vehicleId] as? OpenIncidents.Loaded)?.incidents.orEmpty()
}

data class UndoLocationData(
    val vehicleId: String,
    val previousLocation: Vehicle.LocationModel
)

sealed class HomeEvent {
    data class OnVehicleSwiped(val newIndex: Int) : HomeEvent()
    data class UpdateLocationClicked(val vehicleId: String) : HomeEvent()
    data class UndoLocationClicked(val vehicleId:String, val previousLocation: Vehicle.LocationModel): HomeEvent()
    /** Una ubicación elegida a mano para un vehículo: arrastrando su marcador. */
    data class ManualLocationChosen(
        val vehicleId: String,
        val latitude: Double,
        val longitude: Double
    ) : HomeEvent()
    data class MarkerTapped(val vehicleId: String) : HomeEvent()
    /** Mantener pulsado un punto del mapa: el vehículo elegido pasa ahí (spec 017). */
    data class MapLongPressed(val latitude: Double, val longitude: Double) : HomeEvent()
    /** El mapa acaba de componerse: lo siguiente es un salto, no un vuelo. */
    data object MapShown : HomeEvent()
    data object GoToVehicleClicked : HomeEvent()
    data object PermisionsDenied : HomeEvent()
    data object OpenSettingsClicked : HomeEvent()
    data object CenterMapOnUserClicked : HomeEvent()
    data object SnackBarDismissed : HomeEvent()
}
