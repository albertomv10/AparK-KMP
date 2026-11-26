package com.albertomedina.apark.presentation.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.model.IncidentType
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.DeleteIncidentUseCase
import com.albertomedina.apark.domain.usecase.ObserveIncidentsUseCase
import com.albertomedina.apark.domain.usecase.ReopenIncidentUseCase
import com.albertomedina.apark.domain.usecase.ReportIncidentUseCase
import com.albertomedina.apark.domain.usecase.ResolveIncidentUseCase
import com.albertomedina.apark.utils.catchListenerFailure
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Las incidencias de un vehículo, en su detalle (spec 019).
 *
 * **Las escrituras se lanzan sin esperar**: no terminan hasta que el servidor confirma, y sin conexión
 * no terminan nunca, aunque el caché ya enseñe el cambio. Si una falla, sale un aviso.
 */
class IncidentsViewModel(
    private val authRepository: AuthRepository,
    private val observeIncidents: ObserveIncidentsUseCase,
    private val reportIncident: ReportIncidentUseCase,
    private val resolveIncident: ResolveIncidentUseCase,
    private val reopenIncident: ReopenIncidentUseCase,
    private val deleteIncident: DeleteIncidentUseCase,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncidentsUiState())
    val uiState: StateFlow<IncidentsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authStateChanges.collect { user ->
                _uiState.update { it.copy(currentUserId = user?.uid) }
            }
        }
    }

    fun onEvent(event: IncidentsEvent) {
        when (event) {
            is IncidentsEvent.Started -> start(event.vehicleId)
            IncidentsEvent.ReportOpened -> _uiState.update { it.copy(form = ReportForm()) }
            IncidentsEvent.ReportDismissed -> _uiState.update { it.copy(form = null) }
            is IncidentsEvent.TypeSelected -> updateForm { it.withType(event.type) }
            is IncidentsEvent.TitleChanged -> updateForm { it.withTitle(event.title) }
            is IncidentsEvent.NoteChanged -> updateForm { it.withNote(event.note) }
            IncidentsEvent.ReportConfirmed -> report()
            is IncidentsEvent.Resolve -> resolve(event.incident)
            is IncidentsEvent.Undo -> write { vehicleId -> reopenIncident(vehicleId, event.incidentId) }
            is IncidentsEvent.DeleteRequested -> _uiState.update { it.copy(pendingDelete = event.incident) }
            IncidentsEvent.DeleteDismissed -> _uiState.update { it.copy(pendingDelete = null) }
            IncidentsEvent.DeleteConfirmed -> {
                val incident = _uiState.value.pendingDelete ?: return
                _uiState.update { it.copy(pendingDelete = null) }
                write { vehicleId -> deleteIncident(vehicleId, incident.id) }
            }
            IncidentsEvent.ResolvedToggled -> _uiState.update { it.copy(showResolved = !it.showResolved) }
            IncidentsEvent.MessageShown -> _uiState.update { it.copy(message = null) }
        }
    }

    private fun start(vehicleId: String) {
        if (_uiState.value.vehicleId == vehicleId) return
        _uiState.update { it.copy(vehicleId = vehicleId) }
        viewModelScope.launch {
            observeIncidents(vehicleId)
                // Si la sesión terminó, no es un fallo: la salida la lleva `SessionViewModel`. Con la
                // sesión viva se registra, y la sección dice que no ha podido cargarlas (spec 021).
                .catchListenerFailure({ authRepository.getCurrentUser() != null }) { error ->
                    crashReporter.recordError(error, INCIDENTS_LISTENER_CONTEXT)
                    _uiState.update { it.copy(loadFailed = true) }
                }
                .collect { incidents -> _uiState.update { it.copy(incidents = incidents, loadFailed = false) } }
        }
    }

    private fun report() {
        val state = _uiState.value
        val form = state.form ?: return
        val type = form.type ?: return
        if (!form.canSubmit) return
        val uid = state.currentUserId ?: return
        // El formulario se cierra ya: la incidencia aparece en la lista en cuanto la recoge el caché.
        _uiState.update { it.copy(form = null) }
        write { vehicleId -> reportIncident(vehicleId, uid, type, form.title, form.note) }
    }

    private fun resolve(incident: Incident) {
        val uid = _uiState.value.currentUserId ?: return
        // El aviso con «Deshacer» sale ya, sin esperar a que el servidor confirme.
        _uiState.update { it.copy(message = IncidentsMessage.Resolved(incident.id)) }
        write { vehicleId -> resolveIncident(vehicleId, incident.id, uid) }
    }

    private fun write(block: suspend (vehicleId: String) -> Result<Unit>) {
        val vehicleId = _uiState.value.vehicleId ?: return
        viewModelScope.launch {
            if (block(vehicleId).isFailure) _uiState.update { it.copy(message = IncidentsMessage.Error) }
        }
    }

    private fun updateForm(change: (ReportForm) -> ReportForm) =
        _uiState.update { state -> state.form?.let { state.copy(form = change(it)) } ?: state }
}

/** Agrupa en Crashlytics los fallos de la escucha de incidencias (spec 021). */
private const val INCIDENTS_LISTENER_CONTEXT = "incidents_listener"

data class IncidentsUiState(
    val vehicleId: String? = null,
    val currentUserId: String? = null,
    val incidents: List<Incident> = emptyList(),
    /** La escucha falló con la sesión viva: se dice en la sección, y la lista se queda como estaba. */
    val loadFailed: Boolean = false,
    /** El formulario de reportar, abierto; `null` si está cerrado. */
    val form: ReportForm? = null,
    val pendingDelete: Incident? = null,
    val showResolved: Boolean = false,
    val message: IncidentsMessage? = null
)

/**
 * El formulario de reportar (spec 019). Se ofrecen todos los tipos a la vez, separados por
 * categorías, así que solo hace falta saber cuál se ha elegido: [type], obligatorio; o
 * «Personalizada», con su [title], también obligatorio.
 */
data class ReportForm(
    val type: IncidentType? = null,
    val title: String = "",
    val note: String = ""
) {
    val isCustom: Boolean get() = type == IncidentType.CUSTOM

    /** Hay qué reportar: un tipo, y si es personalizada, con nombre. */
    val canSubmit: Boolean get() = type != null && (!isCustom || title.isNotBlank())

    /**
     * Elegir es obligatorio, así que volver a tocar el elegido no lo quita. Pasar de «Personalizada»
     * a otro tipo conserva el nombre escrito, por si se vuelve; solo se envía si es personalizada.
     */
    fun withType(type: IncidentType): ReportForm = copy(type = type)

    /** Lo que pase del límite no se escribe (criterio 6). */
    fun withTitle(title: String): ReportForm = copy(title = title.take(Incident.TITLE_MAX_LENGTH))

    fun withNote(note: String): ReportForm = copy(note = note.take(Incident.NOTE_MAX_LENGTH))
}

sealed interface IncidentsMessage {
    data class Resolved(val incidentId: String) : IncidentsMessage
    data object Error : IncidentsMessage
}

sealed interface IncidentsEvent {
    data class Started(val vehicleId: String) : IncidentsEvent
    data object ReportOpened : IncidentsEvent
    data object ReportDismissed : IncidentsEvent
    data class TypeSelected(val type: IncidentType) : IncidentsEvent
    data class TitleChanged(val title: String) : IncidentsEvent
    data class NoteChanged(val note: String) : IncidentsEvent
    data object ReportConfirmed : IncidentsEvent
    data class Resolve(val incident: Incident) : IncidentsEvent
    data class Undo(val incidentId: String) : IncidentsEvent
    data class DeleteRequested(val incident: Incident) : IncidentsEvent
    data object DeleteConfirmed : IncidentsEvent
    data object DeleteDismissed : IncidentsEvent
    data object ResolvedToggled : IncidentsEvent
    data object MessageShown : IncidentsEvent
}
