package com.albertomedina.apark.presentation.editvehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.catalog.VehicleCatalog
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.details
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.GetVehicleCatalogUseCase
import com.albertomedina.apark.domain.usecase.GetVehicleListUseCase
import com.albertomedina.apark.domain.usecase.UpdateVehicleUseCase
import com.albertomedina.apark.presentation.vehicleform.VehicleFormEvent
import com.albertomedina.apark.presentation.vehicleform.VehicleFormState
import com.albertomedina.apark.presentation.vehicleform.reduce
import com.albertomedina.apark.presentation.vehicleform.toSave
import com.albertomedina.apark.presentation.vehicleform.withNameError
import com.albertomedina.apark.utils.SnackbarMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Editar un vehículo (spec 015). Solo el dueño llega aquí: el lápiz del detalle no se le enseña a
 * nadie más, y las reglas se lo denegarían igual.
 *
 * El vehículo sale de **la consulta de la lista**, como en el detalle, y no de su documento: un
 * listener por id respondería PERMISSION_DENIED si te quitaran mientras editas. Con la consulta
 * simplemente deja de llegar, y entonces la pantalla se cierra; también si deja de ser tuyo.
 *
 * El formulario se rellena **una sola vez**, con lo primero que llega. Lo que llegue después —que
 * alguien aparque, por ejemplo— no pisa lo que estás escribiendo.
 */
class EditVehicleViewModel(
    private val authRepository: AuthRepository,
    private val getVehicleListUseCase: GetVehicleListUseCase,
    private val updateVehicleUseCase: UpdateVehicleUseCase,
    private val getVehicleCatalogUseCase: GetVehicleCatalogUseCase,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditVehicleUiState())
    val uiState: StateFlow<EditVehicleUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            val catalog = getVehicleCatalogUseCase()
            _uiState.update { it.copy(catalog = catalog) }
        }
    }

    fun onEvent(event: EditVehicleEvent) {
        when (event) {
            is EditVehicleEvent.Load -> observe(event.vehicleId)
            is EditVehicleEvent.Form -> _uiState.update { it.copy(form = reduce(it.form, event.event)) }
            EditVehicleEvent.SaveClicked -> save()
            EditVehicleEvent.SnackBarDismissed -> _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observe(vehicleId: String) {
        if (_uiState.value.vehicleId == vehicleId) return
        observeJob?.cancel()
        _uiState.update { EditVehicleUiState(vehicleId = vehicleId, catalog = it.catalog) }

        observeJob = viewModelScope.launch {
            authRepository.authStateChanges
                .map { it?.uid }
                .distinctUntilChanged()
                .flatMapLatest { uid ->
                    if (uid.isNullOrBlank()) flowOf(null to null)
                    else getVehicleListUseCase(uid).map { vehicles -> uid to vehicles.firstOrNull { it.id == vehicleId } }
                }
                .catch { error ->
                    crashReporter.recordFailure(LOAD_FAILED_CONTEXT, error.message)
                    _uiState.update { it.copy(closeRequested = true) }
                }
                .collect { (uid, vehicle) -> onVehicle(uid, vehicle) }
        }
    }

    private fun onVehicle(uid: String?, vehicle: Vehicle?) {
        if (uid == null) return
        if (vehicle == null || vehicle.ownerId != uid) {
            // Se ha borrado, te han quitado o ya no es tuyo: aquí no queda nada que editar.
            if (!_uiState.value.isSaving) _uiState.update { it.copy(closeRequested = true) }
            return
        }
        if (_uiState.value.isLoaded) return
        _uiState.update {
            it.copy(isLoaded = true, form = VehicleFormState(details = vehicle.details(), showMore = true))
        }
    }

    private fun save() {
        val state = _uiState.value
        val vehicleId = state.vehicleId ?: return
        if (!state.isLoaded || state.isSaving) return
        val details = state.form.toSave()
        if (details == null) {
            _uiState.update { it.copy(form = it.form.withNameError()) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            updateVehicleUseCase(vehicleId, details).fold(
                onSuccess = { _uiState.update { it.copy(isSaving = false, closeRequested = true) } },
                onFailure = { error ->
                    crashReporter.recordFailure(SAVE_FAILED_CONTEXT, error.message)
                    _uiState.update { it.copy(isSaving = false, snackbarMessage = SnackbarMessage.Error(ERROR_SAVE_KEY)) }
                }
            )
        }
    }

    companion object {
        const val ERROR_SAVE_KEY = "edit_vehicle_error"
        private const val LOAD_FAILED_CONTEXT = "edit_vehicle_load_failed"
        private const val SAVE_FAILED_CONTEXT = "edit_vehicle_save_failed"
    }
}

data class EditVehicleUiState(
    val vehicleId: String? = null,
    val isLoaded: Boolean = false,
    val form: VehicleFormState = VehicleFormState(),
    val catalog: VehicleCatalog = VehicleCatalog.EMPTY,
    val isSaving: Boolean = false,
    /** Guardado, o ya no hay nada que editar: la pantalla se cierra. */
    val closeRequested: Boolean = false,
    val snackbarMessage: SnackbarMessage? = null
)

sealed interface EditVehicleEvent {
    data class Load(val vehicleId: String) : EditVehicleEvent
    data class Form(val event: VehicleFormEvent) : EditVehicleEvent
    data object SaveClicked : EditVehicleEvent
    data object SnackBarDismissed : EditVehicleEvent
}
