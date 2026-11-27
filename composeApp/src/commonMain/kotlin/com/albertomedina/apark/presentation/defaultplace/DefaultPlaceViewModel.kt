package com.albertomedina.apark.presentation.defaultplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.GetVehicleListUseCase
import com.albertomedina.apark.domain.usecase.LocateUserUseCase
import com.albertomedina.apark.domain.usecase.RemoveDefaultPlaceUseCase
import com.albertomedina.apark.domain.usecase.SetDefaultPlaceUseCase
import com.albertomedina.apark.presentation.components.DEFAULT_MAP_ZOOM
import com.albertomedina.apark.presentation.components.DefaultMapCenter
import com.albertomedina.apark.presentation.components.GeoPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Elegir la ubicación predeterminada de un vehículo (spec 024).
 *
 * El vehículo se lee de la consulta de la lista, nunca por id: si te sacan del vehículo, una escucha
 * por id respondería PERMISSION_DENIED (AGENTS). Basta con la primera respuesta, que es la del caché.
 *
 * **Guardar y quitar no esperan al servidor**: sin conexión, la escritura no termina nunca, aunque el
 * caché ya la enseñe. Se lanzan y se vuelve al detalle. Si el servidor la rechaza, el caché la deshace
 * solo, y se registra el fallo.
 */
class DefaultPlaceViewModel(
    private val authRepository: AuthRepository,
    private val getVehicleListUseCase: GetVehicleListUseCase,
    private val locateUserUseCase: LocateUserUseCase,
    private val setDefaultPlaceUseCase: SetDefaultPlaceUseCase,
    private val removeDefaultPlaceUseCase: RemoveDefaultPlaceUseCase,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(DefaultPlaceUiState())
    val uiState: StateFlow<DefaultPlaceUiState> = _uiState.asStateFlow()

    fun onEvent(event: DefaultPlaceEvent) {
        when (event) {
            is DefaultPlaceEvent.Load -> load(event.vehicleId)
            is DefaultPlaceEvent.CenterChanged -> _uiState.update { it.copy(center = event.point) }
            is DefaultPlaceEvent.NameChanged -> _uiState.update {
                it.copy(name = event.name.take(Vehicle.DefaultPlace.NAME_MAX_LENGTH))
            }
            is DefaultPlaceEvent.SaveClicked -> save(event.defaultName)
            DefaultPlaceEvent.RemoveClicked -> remove()
        }
    }

    private fun load(vehicleId: String) {
        if (_uiState.value.vehicleId == vehicleId) return
        _uiState.update { DefaultPlaceUiState(vehicleId = vehicleId) }
        viewModelScope.launch {
            val uid = authRepository.getCurrentUser()?.uid ?: return@launch
            // La primera respuesta de la lista. Si la escucha falla, se recoge aquí y no cierra la
            // app (spec 021); con la sesión viva, además, se registra.
            val vehicle = try {
                getVehicleListUseCase(uid).first().firstOrNull { it.id == vehicleId }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (authRepository.getCurrentUser() != null) crashReporter.recordError(e, LOAD_CONTEXT)
                null
            }
            if (vehicle == null) {
                // Ya no está: te han sacado, o se ha borrado. Aquí no queda nada que elegir.
                _uiState.update { it.copy(closeRequested = true) }
                return@launch
            }
            val existing = vehicle.defaultPlace
            val parked = vehicle.lastLocation?.takeIf { it.timestamp != 0L }
            // Dónde nace el mapa: donde ya está la ubicación; si no, donde está aparcado, que suele
            // ser justo la plaza; si no, donde estás tú; y si nada de eso, España entera.
            val (initial, zoom) = when {
                existing != null -> GeoPoint(existing.latitude, existing.longitude) to PLACE_ZOOM
                parked != null -> GeoPoint(parked.latitude, parked.longitude) to PLACE_ZOOM
                else -> locateUserUseCase()?.let { GeoPoint(it.latitude, it.longitude) to PLACE_ZOOM }
                    ?: (DefaultMapCenter to DEFAULT_MAP_ZOOM)
            }
            _uiState.update {
                it.copy(
                    vehicleName = vehicle.name,
                    existing = existing,
                    name = existing?.name,
                    initial = initial,
                    initialZoom = zoom,
                    center = initial
                )
            }
        }
    }

    private fun save(defaultName: String) {
        val state = _uiState.value
        val vehicleId = state.vehicleId ?: return
        val center = state.center ?: return
        _uiState.update { it.copy(closeRequested = true) }
        viewModelScope.launch {
            setDefaultPlaceUseCase(vehicleId, state.name ?: defaultName, center.latitude, center.longitude, defaultName)
                .onFailure { crashReporter.recordError(it, SAVE_CONTEXT) }
        }
    }

    private fun remove() {
        val vehicleId = _uiState.value.vehicleId ?: return
        _uiState.update { it.copy(closeRequested = true) }
        viewModelScope.launch {
            removeDefaultPlaceUseCase(vehicleId).onFailure { crashReporter.recordError(it, SAVE_CONTEXT) }
        }
    }

    private companion object {
        /** De calle, de cerca: lo justo para acertar con una plaza. */
        const val PLACE_ZOOM = 18f
        const val LOAD_CONTEXT = "default_place_load"
        const val SAVE_CONTEXT = "default_place_save"
    }
}

data class DefaultPlaceUiState(
    val vehicleId: String? = null,
    val vehicleName: String = "",
    /** La que ya tiene, si tiene: entonces se puede quitar. */
    val existing: Vehicle.DefaultPlace? = null,
    /** El nombre escrito; `null` mientras no se toca, y entonces se propone «Garaje». */
    val name: String? = null,
    /** Dónde nace el mapa; `null` mientras carga. */
    val initial: GeoPoint? = null,
    val initialZoom: Float = DEFAULT_MAP_ZOOM,
    /** El punto elegido: el centro del mapa la última vez que se paró. */
    val center: GeoPoint? = null,
    /** Hay que volver al detalle: se ha guardado, se ha quitado, o el vehículo ya no está. */
    val closeRequested: Boolean = false
)

sealed interface DefaultPlaceEvent {
    data class Load(val vehicleId: String) : DefaultPlaceEvent
    data class CenterChanged(val point: GeoPoint) : DefaultPlaceEvent
    data class NameChanged(val name: String) : DefaultPlaceEvent
    /** [defaultName] es «Garaje» en el idioma de quien mira, por si deja el nombre vacío. */
    data class SaveClicked(val defaultName: String) : DefaultPlaceEvent
    data object RemoveClicked : DefaultPlaceEvent
}
