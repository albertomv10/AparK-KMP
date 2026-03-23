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

    private fun loadMockData() {
        val mockVehicles = listOf(
            Vehicle(
                id = "v1",
                name = "Mi Coche",
                model = "Seat León",
                licensePlate = "1234 ABC",
                color = "Rojo",
                ownerId = "user123",
                lastLocation = Vehicle.LocationModel(
                    latitude = 40.4168,
                    longitude = -3.7038,
                    timestamp = Clock.System.now().toEpochMilliseconds() - 3600000,
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
                    latitude = 41.3851,
                    longitude = 2.1734,
                    timestamp = Clock.System.now().toEpochMilliseconds() - 86400000,
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
                    latitude = 39.4699,
                    longitude = -0.3774,
                    timestamp = Clock.System.now().toEpochMilliseconds() - 120000,
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

    fun runDebugDatabaseSeed() {
        val targetOwnerId = "u0uGzvEAfbZNxY91KwHKXwcSXoS2"
        val testUserEmail = "debug@apark.com"
        val currentTime = Clock.System.now().toEpochMilliseconds()

        val murciaVehicles = listOf(
            Vehicle(
                name = "Coche Diario", model = "Seat León", licensePlate = "1234 ABC", color = "Rojo", ownerId = targetOwnerId,
                lastLocation = Vehicle.LocationModel(37.9922, -1.1307, currentTime, testUserEmail) // Plaza Circular
            ),
            Vehicle(
                name = "Coche Trabajo", model = "Renault Kangoo", licensePlate = "5678 DFG", color = "Blanco", ownerId = targetOwnerId,
                lastLocation = Vehicle.LocationModel(37.9838, -1.1275, currentTime - 3600000, testUserEmail) // Catedral de Murcia
            ),
            Vehicle(
                name = "La Moto", model = "Yamaha MT-07", licensePlate = "9012 HJK", color = "Negro", ownerId = targetOwnerId,
                lastLocation = Vehicle.LocationModel(37.9858, -1.1252, currentTime - 7200000, testUserEmail) // Campus La Merced
            ),
            Vehicle(
                name = "Coche Viajes", model = "Volkswagen Golf", licensePlate = "3456 LMN", color = "Azul", ownerId = targetOwnerId,
                lastLocation = Vehicle.LocationModel(37.9748, -1.1312, currentTime - 86400000, testUserEmail) // Estación El Carmen
            ),
            Vehicle(
                name = "Coche Mujer", model = "Toyota Yaris", licensePlate = "7890 PQR", color = "Gris", ownerId = targetOwnerId,
                lastLocation = Vehicle.LocationModel(37.9865, -1.1315, currentTime, testUserEmail) // Gran Vía (Corte Inglés)
            ),
            Vehicle(
                name = "Furgoneta Camper", model = "VW California", licensePlate = "2345 STV", color = "Verde", ownerId = targetOwnerId,
                lastLocation = Vehicle.LocationModel(37.9985, -1.1385, currentTime - 120000, testUserEmail) // Zona ZigZag
            ),
            Vehicle(
                name = "Patinete Eléctrico", model = "Xiaomi Pro 2", licensePlate = "S/N", color = "Negro", ownerId = targetOwnerId,
                lastLocation = Vehicle.LocationModel(37.9815, -1.1370, currentTime, testUserEmail) // Paseo del Malecón
            ),
            Vehicle(
                name = "Coche Clásico", model = "Mini Cooper", licensePlate = "M-1234-AB", color = "Verde Inglés", ownerId = targetOwnerId,
                lastLocation = Vehicle.LocationModel(37.9790, -1.1300, currentTime - 5000000, testUserEmail) // Jardín de Floridablanca
            ),
            Vehicle(
                name = "Coche Empresa", model = "Audi A3", licensePlate = "6789 WXY", color = "Plata", ownerId = targetOwnerId,
                lastLocation = Vehicle.LocationModel(37.9930, -1.1240, currentTime - 300000, testUserEmail) // Hospital Morales Meseguer
            ),
            Vehicle(
                name = "Bici Montaña", model = "Orbea Alma", licensePlate = "S/N", color = "Naranja", ownerId = targetOwnerId,
                lastLocation = Vehicle.LocationModel(37.9805, -1.1150, currentTime, testUserEmail) // Auditorio Víctor Villegas
            )
        )

        viewModelScope.launch {
            try {
                // Activar estado de carga si quieres verlo en la UI
                // _uiState.update { it.copy(isLoading = true) }

                println("⏳ Iniciando subida masiva de 10 vehículos a Firebase...")

                murciaVehicles.forEach { vehicle ->
                    // ¡AQUÍ DEBES LLAMAR A TU REPOSITORIO O USECASE PARA CREAR EL VEHÍCULO!
                    // Por ejemplo:
                    // addVehicleUseCase(vehicle)
                    // o
                    // firestoreRepository.saveVehicle(vehicle)

                    println("🚗 Subido: ${vehicle.name} en ${vehicle.model}")
                }

                println("✅ ¡Éxito! Todos los vehículos ficticios se han plantado en Murcia.")

            } catch (e: Exception) {
                println("🚨 Error subiendo los datos falsos: ${e.message}")
            } finally {
                // _uiState.update { it.copy(isLoading = false) }
            }
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
