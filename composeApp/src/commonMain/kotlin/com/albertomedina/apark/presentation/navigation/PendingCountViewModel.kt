package com.albertomedina.apark.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.openCount
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.GetVehicleListUseCase
import com.albertomedina.apark.domain.usecase.ObserveOpenIncidentsUseCase
import com.albertomedina.apark.utils.catchListenerFailure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * La chapita de la pestaña *Vehículos* (spec 020): cuántas incidencias abiertas tienen tus vehículos.
 * Vive en `TabShell`, como `SessionViewModel`, así que se ve desde cualquier pestaña y desde un
 * detalle.
 *
 * Cuenta **lo que está sin resolver, no lo que no has visto**: entrar en la pestaña no la borra; solo
 * baja al resolver o borrar una incidencia.
 *
 * Abre sus propias escuchas —la de los vehículos y una por vehículo—, pero son las mismas consultas
 * que *Inicio* y *Vehículos*, y el SDK de Firestore las comparte.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PendingCountViewModel(
    private val authRepository: AuthRepository,
    getVehicleListUseCase: GetVehicleListUseCase,
    observeOpenIncidents: ObserveOpenIncidentsUseCase,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val isSignedIn = { authRepository.getCurrentUser() != null }

    /** `0` sin incidencias, sin sesión, o mientras carga: entonces no hay chapita. */
    val openIncidents: StateFlow<Int> = authRepository.authStateChanges
        .map { it?.uid }
        // También emite al refrescarse el token, cada hora más o menos; eso no es un cambio.
        .distinctUntilChanged()
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                // Dentro del `flatMapLatest`, para que un fallo no corte la escucha de la sesión
                // (spec 021). Si falla, la chapita se queda con lo último que supo.
                getVehicleListUseCase(uid).catchListenerFailure(isSignedIn) { error ->
                    crashReporter.recordError(error, VEHICLES_LISTENER_CONTEXT)
                }
            }
        }
        .map { vehicles -> vehicles.mapTo(mutableSetOf(), Vehicle::id) }
        // Las escuchas de incidencias solo se reabren si cambia el conjunto de vehículos.
        .distinctUntilChanged()
        .flatMapLatest { ids ->
            observeOpenIncidents(ids, isSignedIn) { error ->
                crashReporter.recordError(error, INCIDENTS_LISTENER_CONTEXT)
            }.map { open -> openCount(ids, open) }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = 0)

    private companion object {
        const val VEHICLES_LISTENER_CONTEXT = "pending_count_vehicles_listener"
        const val INCIDENTS_LISTENER_CONTEXT = "pending_count_incidents_listener"
    }
}
