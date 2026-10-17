package com.albertomedina.apark.presentation.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.GetVehicleListUseCase
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

class VehiclesViewModel(
    private val authRepository: AuthRepository,
    private val getVehicleListUseCase: GetVehicleListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehiclesUiState())
    val uiState: StateFlow<VehiclesUiState> = _uiState.asStateFlow()

    init {
        // Igual que en Home: se observa el estado de auth en vez de cargar una vez en `init`, para
        // que los datos cambien con el usuario y no se quede lo del anterior.
        observeAuthState()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authStateChanges
                .map { it?.uid }
                // `authStateChanges` también emite al refrescarse el token, cada hora más o menos,
                // y no solo al entrar o salir. Sin este filtro cada refresco cancelaba los
                // listeners de Firestore y los volvía a abrir para el mismo usuario.
                .distinctUntilChanged()
                .flatMapLatest { userId ->
                    if (userId.isNullOrBlank()) flowOf(emptyList()) else getVehicleListUseCase(userId)
                }
                .collect { vehicles -> _uiState.update { it.copy(vehicles = vehicles, isLoading = false) } }
        }
    }
}

data class VehiclesUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val isLoading: Boolean = true
)
