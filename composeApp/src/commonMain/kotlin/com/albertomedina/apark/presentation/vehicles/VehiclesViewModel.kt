package com.albertomedina.apark.presentation.vehicles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.model.OpenIncidents
import com.albertomedina.apark.domain.model.Pending
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.pendingOf
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.GetVehicleListUseCase
import com.albertomedina.apark.domain.usecase.ObserveOpenIncidentsUseCase
import com.albertomedina.apark.domain.usecase.SetVehicleOrderUseCase
import com.albertomedina.apark.utils.catchListenerFailure
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
    private val getVehicleListUseCase: GetVehicleListUseCase,
    private val setVehicleOrderUseCase: SetVehicleOrderUseCase,
    private val observeOpenIncidents: ObserveOpenIncidentsUseCase,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehiclesUiState())
    val uiState: StateFlow<VehiclesUiState> = _uiState.asStateFlow()

    init {
        // Igual que en Home: se observa el estado de auth en vez de cargar una vez en `init`, para
        // que los datos cambien con el usuario y no se quede lo del anterior.
        observeAuthState()
        observeIncidents()
    }

    /**
     * Las incidencias abiertas de cada vehículo, para «Pendiente» (spec 020). Las escuchas se abren
     * según **el conjunto de ids** de la lista: se reabren cuando alguien entra o sale de un vehículo,
     * no cada vez que alguien aparca o se reordena. El mapa no se vacía al reabrir: mientras llega lo
     * de un vehículo nuevo se sigue viendo lo de los demás, y `pendingOf` ignora lo que sobre.
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
                // `authStateChanges` también emite al refrescarse el token, cada hora más o menos,
                // y no solo al entrar o salir. Sin este filtro cada refresco cancelaba los
                // listeners de Firestore y los volvía a abrir para el mismo usuario.
                .distinctUntilChanged()
                .flatMapLatest { userId ->
                    // Otro usuario, otra lista: lo que se estuviera ordenando ya no vale, ni lo que se
                    // supiera de sus incidencias.
                    _uiState.update {
                        it.copy(
                            currentUserId = userId,
                            isReordering = false,
                            orderOverride = null,
                            openIncidents = emptyMap()
                        )
                    }
                    if (userId.isNullOrBlank()) {
                        flowOf(emptyList())
                    } else {
                        // Si la escucha falla, se queda la lista que hubiera y se avisa (spec 021). Deja
                        // de cargar: si no había nada en caché, la lista ya no va a llegar.
                        getVehicleListUseCase(userId)
                            .catchListenerFailure({ authRepository.getCurrentUser() != null }) { error ->
                                crashReporter.recordError(error, VEHICLES_LISTENER_CONTEXT)
                                _uiState.update { it.copy(isLoading = false, errorKey = ERROR_LOAD_KEY) }
                            }
                    }
                }
                .collect { vehicles -> _uiState.update { it.withVehicles(vehicles) } }
        }
    }

    fun onEvent(event: VehiclesEvent) {
        when (event) {
            VehiclesEvent.ReorderStarted -> _uiState.update {
                if (it.shownVehicles.size < 2) it
                else it.copy(isReordering = true, orderOverride = it.shownVehicles.map(Vehicle::id))
            }

            is VehiclesEvent.Moved -> _uiState.update {
                if (!it.isReordering) it
                else it.copy(orderOverride = it.shownVehicles.map(Vehicle::id).moved(event.from, event.to))
            }

            VehiclesEvent.ReorderFinished -> finishReorder()

            VehiclesEvent.MessageShown -> _uiState.update { it.copy(errorKey = null) }
        }
    }

    private fun finishReorder() {
        val state = _uiState.value
        if (!state.isReordering) return
        val ids = state.shownVehicles.map(Vehicle::id)

        // Sin cambios no se escribe nada.
        if (ids == state.vehicles.map(Vehicle::id)) {
            _uiState.update { it.copy(isReordering = false, orderOverride = null) }
            return
        }
        _uiState.update { it.copy(isReordering = false) }

        val userId = state.currentUserId ?: return
        // Sin esperar: la escritura no termina hasta que el servidor la confirma, y sin conexión no
        // termina nunca. Mientras tanto el orden nuevo se sigue viendo por el override, y en cuanto
        // el caché local lo devuelve, `withVehicles` lo suelta.
        viewModelScope.launch {
            val result = setVehicleOrderUseCase(userId, ids)
            _uiState.update {
                // Si entretanto se ha vuelto a ordenar, ese override es otro y no se toca.
                val settled = if (it.orderOverride == ids) it.copy(orderOverride = null) else it
                if (result.isFailure) settled.copy(errorKey = ERROR_REORDER_KEY) else settled
            }
        }
    }

    companion object {
        const val ERROR_REORDER_KEY = "reorder_error"
        const val ERROR_LOAD_KEY = "load_error"
        private const val VEHICLES_LISTENER_CONTEXT = "vehicles_list_listener"
        private const val INCIDENTS_LISTENER_CONTEXT = "vehicles_incidents_listener"
    }
}

data class VehiclesUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val isLoading: Boolean = true,
    val currentUserId: String? = null,
    val isReordering: Boolean = false,
    /**
     * El orden que se está poniendo, o que acaba de guardarse y todavía no ha vuelto de Firestore.
     * Mientras exista manda sobre el de [vehicles]; los vehículos que no estén en él van al final.
     */
    val orderOverride: List<String>? = null,
    val errorKey: String? = null,
    /** Lo que se sabe de las incidencias abiertas de cada vehículo, por su id (spec 020). */
    val openIncidents: Map<String, OpenIncidents> = emptyMap()
) {
    /** Lo que se ve: la lista de Firestore, en el orden que se está poniendo si se está ordenando. */
    val shownVehicles: List<Vehicle>
        get() = orderOverride?.let { vehicles.inOrder(it) } ?: vehicles

    /** «Pendiente»: lo abierto de los vehículos de la lista, y si se sabe todo. */
    val pending: Pending
        get() = pendingOf(vehicles, openIncidents)
}

/**
 * Llega una lista nueva. Fuera del modo ordenar, si ya viene en el orden que se guardó, el override
 * sobra: es el caché local devolviendo la escritura, con o sin conexión.
 */
private fun VehiclesUiState.withVehicles(vehicles: List<Vehicle>): VehiclesUiState {
    val override = orderOverride
    val settled = !isReordering && override != null &&
        vehicles.map(Vehicle::id) == vehicles.inOrder(override).map(Vehicle::id)
    return copy(vehicles = vehicles, isLoading = false, orderOverride = if (settled) null else override)
}

sealed interface VehiclesEvent {
    data object ReorderStarted : VehiclesEvent
    data class Moved(val from: Int, val to: Int) : VehiclesEvent
    data object ReorderFinished : VehiclesEvent
    data object MessageShown : VehiclesEvent
}
