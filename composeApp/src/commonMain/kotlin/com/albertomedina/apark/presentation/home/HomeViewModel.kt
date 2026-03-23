package com.albertomedina.apark.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.data.repository.FirestoreRepository
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.GetVehicleListUseCase
import com.albertomedina.apark.domain.usecase.UpdateVehicleLocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

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
            is HomeEvent.CenterMapOnUserClicked -> {
                _uiState.update { it.copy(centerCameraTrigger = it.centerCameraTrigger + 1)}
            }
            is HomeEvent.SignOutClicked -> {
                _uiState.update { it.copy(shouldNavigateToLogin = true) }
            }
            is HomeEvent.NavigationHandled -> {
                _uiState.update { it.copy(shouldNavigateToLogin = false) }
            }
        }
    }

    fun loadVehicles(){
        val userId = authRepository.getCurrentUser()?.uid
        
        if (userId.isNullOrBlank()) {
            println("HomeViewModel: No se pueden cargar vehículos porque el userId es nulo o vacío")
            return
        }

        viewModelScope.launch {
            getVehicleListUseCase(userId).collect { vehicleList ->
                _uiState.update { it.copy(vehicles = vehicleList) }
            }
        }
    }

    private fun updateVehicleLocation(vehicleId: String) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val result = updateVehicleLocationUseCase(vehicleId)

            result.fold(
                onSuccess = {
                    println("✅ Ubicación actualizada con éxito")
                },
                onFailure = { error ->
                    println("🚨 Error al actualizar ubicación: ${error.message}")
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
    val centerCameraTrigger: Int = 0,
    val shouldNavigateToLogin: Boolean = false
)

sealed class HomeEvent {
    data class OnVehicleSwiped(val newIndex: Int) : HomeEvent()
    data class UpdateLocationClicked(val vehicleId: String) : HomeEvent()
    data class VehicleDetailsClicked(val vehicleId: String) : HomeEvent()
    data object CenterMapOnUserClicked : HomeEvent()
    data object AddVehicleClicked : HomeEvent()
    data object SignOutClicked : HomeEvent()
    data object NavigationHandled : HomeEvent()
}
