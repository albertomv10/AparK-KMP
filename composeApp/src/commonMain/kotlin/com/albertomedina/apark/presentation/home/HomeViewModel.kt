package com.albertomedina.apark.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.GetVehicleListUseCase
import com.albertomedina.apark.domain.usecase.UpdateVehicleLocationUseCase
import com.albertomedina.apark.utils.SnackbarMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val updateVehicleLocationUseCase: UpdateVehicleLocationUseCase,
    private val getVehicleListUseCase: GetVehicleListUseCase,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
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
                    // Keep the uid around: the UI needs it to tell owner from shared member.
                    _uiState.update { it.copy(currentUserId = userId) }
                    // Ata los informes de fallo a un usuario — el uid, nunca el email — y los
                    // desata al cerrar sesión. Va aquí porque es donde el estado de auth ya fluye.
                    crashReporter.setUserId(userId)
                    if (!userId.isNullOrBlank()) {
                        getVehicleListUseCase(userId)
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { vehicleList ->
                    _uiState.update { it.withVehicles(vehicleList) }
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
            }

            is HomeEvent.UpdateLocationClicked -> {
                updateVehicleLocation(event.vehicleId)
            }

            is HomeEvent.UndoLocationClicked -> {
                undoVehicleLocation(event.vehicleId, event.previousLocation)
            }

            is HomeEvent.OnMarkerDragged -> {
                saveManualLocation(event.vehicleId, event.latitude, event.longitude)
            }

            is HomeEvent.CenterMapOnUserClicked -> {
                _uiState.update { it.copy(centerCameraTrigger = it.centerCameraTrigger + 1) }
            }

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

data class HomeUiState(
    val currentUserId: String? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleIndex: Int = 0,
    /** El vehículo que enseña el carrusel, para seguirlo si cambia de posición; `null` en la tarjeta «+». */
    val selectedVehicleId: String? = null,
    val isLoading: Boolean = false,
    val updatingVehicleId: String? = null,
    val centerCameraTrigger: Int = 0,
    val openSettingsTrigger: Int = 0,
    val locationUpdateSuccessData: UndoLocationData? = null,
    val snackbarMessage: SnackbarMessage? = null
)

data class UndoLocationData(
    val vehicleId: String,
    val previousLocation: Vehicle.LocationModel
)

sealed class HomeEvent {
    data class OnVehicleSwiped(val newIndex: Int) : HomeEvent()
    data class UpdateLocationClicked(val vehicleId: String) : HomeEvent()
    data class UndoLocationClicked(val vehicleId:String, val previousLocation: Vehicle.LocationModel): HomeEvent()
    data class OnMarkerDragged(
        val vehicleId: String,
        val latitude: Double,
        val longitude: Double
    ) : HomeEvent()
    data object PermisionsDenied : HomeEvent()
    data object OpenSettingsClicked : HomeEvent()
    data object CenterMapOnUserClicked : HomeEvent()
    data object SnackBarDismissed : HomeEvent()
}
