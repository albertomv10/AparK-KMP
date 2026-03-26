package com.albertomedina.apark.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.GetVehicleListUseCase
import com.albertomedina.apark.domain.usecase.UpdateVehicleLocationUseCase
import com.albertomedina.apark.utils.SnackbarMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val updateVehicleLocationUseCase: UpdateVehicleLocationUseCase,
    private val getVehicleListUseCase: GetVehicleListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadVehicles()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnVehicleSwiped -> {
                _uiState.update { it.copy(selectedVehicleIndex = event.newIndex) }
            }
            is HomeEvent.UpdateLocationClicked -> {
                updateVehicleLocation(event.vehicleId)
            }
            is HomeEvent.VehicleDetailsClicked -> {
                println("Navegando a detalles del vehículo: ${event.vehicleId}")
            }
            is HomeEvent.AddVehicleClicked -> {
                println("Navegando a crear nuevo vehículo")
            }
            is HomeEvent.UndoLocationClicked -> {
                undoVehicleLocation(event.vehicleId, event.previousLocation)
            }
            is HomeEvent.CenterMapOnUserClicked -> {
                _uiState.update { it.copy(centerCameraTrigger = it.centerCameraTrigger + 1)}
            }
            is HomeEvent.SignOutClicked -> {
                _uiState.update { it.copy(shouldNavigateToLogin = true) }
            }
            is HomeEvent.NavigationHandled -> {
                _uiState.update { it.copy(shouldNavigateToLogin = false) }
            }
            is HomeEvent.SnackBarDismissed -> {
                _uiState.update { it.copy(locationUpdateSuccessData = null, snackbarMessage = null) }
            }
        }
    }

    fun loadVehicles(){
        val userId = authRepository.getCurrentUser()?.uid
        
        if (userId.isNullOrBlank()) {
            return
        }

        viewModelScope.launch {
            getVehicleListUseCase(userId).collect { vehicleList ->
                _uiState.update { it.copy(vehicles = vehicleList) }
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
                    onSuccess = {
                        if (previousLocation != null) {
                            _uiState.update { state ->
                                state.copy(
                                    locationUpdateSuccessData = UndoLocationData(vehicleId, previousLocation),
                                    snackbarMessage = SnackbarMessage.Success("success_location_updated")
                                )
                            }
                        }
                    },
                    onFailure = {
                        _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_location_save")) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_gps_permissions"))}
            } finally {
                _uiState.update { it.copy(isLoading = false, updatingVehicleId = null) }
            }
        }
    }

    private fun undoVehicleLocation(vehicleId: String, previousLocation: Vehicle.LocationModel) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = updateVehicleLocationUseCase(vehicleId, previousLocation)

            result.fold(
                onSuccess = {
                    // Opcional: mostrar mensaje de que se deshizo correctamente
                },
                onFailure = {
                    _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_undo_failed")) }
                }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}

data class HomeUiState(
    val userEmail: String = "",
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleIndex: Int = 0,
    val isLoading: Boolean = false,
    val updatingVehicleId: String? = null,
    val centerCameraTrigger: Int = 0,
    val shouldNavigateToLogin: Boolean = false,
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
    data class VehicleDetailsClicked(val vehicleId: String) : HomeEvent()
    data class UndoLocationClicked(val vehicleId:String, val previousLocation: Vehicle.LocationModel): HomeEvent()
    data object CenterMapOnUserClicked : HomeEvent()
    data object AddVehicleClicked : HomeEvent()
    data object SignOutClicked : HomeEvent()
    data object NavigationHandled : HomeEvent()
    data object SnackBarDismissed : HomeEvent()
}
