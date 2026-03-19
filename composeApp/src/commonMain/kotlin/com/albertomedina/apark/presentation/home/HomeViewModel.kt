package com.albertomedina.apark.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.model.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class HomeViewModel(
    // private val authRepository: AuthRepository // Comentado temporalmente para la prueba
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Cargamos los datos falsos nada más arrancar
        loadMockData()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnVehicleSwiped -> {
                _uiState.update { it.copy(selectedVehicleIndex = event.newIndex) }
            }
            is HomeEvent.UpdateLocationClicked -> {
                // Simulamos que actualiza la ubicación del coche
                println("Actualizando ubicación del vehículo: ${event.vehicleId}")
            }
            is HomeEvent.VehicleDetailsClicked -> {
                println("Navegando a detalles del vehículo: ${event.vehicleId}")
            }
            is HomeEvent.AddVehicleClicked -> {
                println("Navegando a crear nuevo vehículo")
            }
            is HomeEvent.SignOutClicked -> {
                // Simulamos el cierre de sesión
                _uiState.update { it.copy(shouldNavigateToLogin = true) }
            }
            is HomeEvent.NavigationHandled -> {
                _uiState.update { it.copy(shouldNavigateToLogin = false) }
            }
        }
    }

    private fun loadMockData() {
        // Tres vehículos de prueba con ubicaciones falsas y timestamps actuales
        val mockVehicles = listOf(
            Vehicle(
                id = "v1",
                name = "Mi Coche",
                model = "Seat León",
                licensePlate = "1234 ABC",
                color = "Rojo",
                ownerId = "user123",
                lastLocation = Vehicle.LocationModel(
                    latitude = 40.4168, // Madrid
                    longitude = -3.7038,
                    timestamp = Clock.System.now().toEpochMilliseconds() - 3600000, // Hace 1 hora
                    user = "Alberto"
                )
            ),
            Vehicle(
                id = "v2",
                name = "Coche Trabajo",
                model = "Renault Kangoo",
                licensePlate = "9876 XYZ",
                color = "Blanco",
                ownerId = "user123",
                lastLocation = Vehicle.LocationModel(
                    latitude = 41.3851, // Barcelona
                    longitude = 2.1734,
                    timestamp = Clock.System.now().toEpochMilliseconds() - 86400000, // Hace 1 día
                    user = "Alberto"
                )
            ),
            Vehicle(
                id = "v3",
                name = "La Moto",
                model = "Yamaha MT-07",
                licensePlate = "5555 MTO",
                color = "Negro",
                ownerId = "user123",
                lastLocation = Vehicle.LocationModel(
                    latitude = 39.4699, // Valencia
                    longitude = -0.3774,
                    timestamp = Clock.System.now().toEpochMilliseconds() - 120000, // Hace 2 minutos
                    user = "Alberto"
                )
            )
        )

        _uiState.update {
            it.copy(
                userEmail = "alberto@apark.com",
                vehicles = mockVehicles,
                selectedVehicleIndex = 0
            )
        }
    }
}

// ESTADO
data class HomeUiState(
    val userEmail: String = "",
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleIndex: Int = 0, // 👈 Clave para saber qué coche enfoca el mapa
    val isLoading: Boolean = false,
    val shouldNavigateToLogin: Boolean = false
)

// EVENTOS
sealed class HomeEvent {
    // Cuando el usuario desliza el dedo a la siguiente tarjeta
    data class OnVehicleSwiped(val newIndex: Int) : HomeEvent()

    // Cuando el usuario pulsa "Aparcar aquí" en la tarjeta
    data class UpdateLocationClicked(val vehicleId: String) : HomeEvent()

    // Cuando pulsa la tarjeta para ver detalles
    data class VehicleDetailsClicked(val vehicleId: String) : HomeEvent()

    // Cuando pulsa el FAB para crear uno nuevo
    data object AddVehicleClicked : HomeEvent()

    data object SignOutClicked : HomeEvent()
    data object NavigationHandled : HomeEvent()
}