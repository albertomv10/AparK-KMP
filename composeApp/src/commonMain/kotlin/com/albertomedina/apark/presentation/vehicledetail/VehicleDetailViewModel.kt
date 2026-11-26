package com.albertomedina.apark.presentation.vehicledetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.model.NotAVehicleMemberException
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.VehicleInvite
import com.albertomedina.apark.domain.model.VehicleMember
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.CreateVehicleInviteUseCase
import com.albertomedina.apark.domain.usecase.DeleteVehicleUseCase
import com.albertomedina.apark.domain.usecase.GetVehicleListUseCase
import com.albertomedina.apark.domain.usecase.GetVehicleMembersUseCase
import com.albertomedina.apark.domain.usecase.RemoveUserFromVehicleUseCase
import com.albertomedina.apark.domain.usecase.RemoveVehicleMemberUseCase
import com.albertomedina.apark.domain.usecase.TransferVehicleOwnershipUseCase
import com.albertomedina.apark.utils.SnackbarMessage
import com.albertomedina.apark.utils.catchListenerFailure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * El detalle de un vehículo: dónde está, quién lo comparte y, para el dueño, quitar a alguien o
 * pasarle la propiedad (spec 014).
 *
 * El vehículo llega de **la misma consulta que usa el mapa**, filtrada por id, y no de un listener
 * sobre su documento. Ese listener devolvería PERMISSION_DENIED en cuanto te quitaran; la consulta,
 * en cambio, simplemente deja de traerlo, y eso es lo que permite decir «ya no tienes acceso» en vez
 * de fallar.
 */
class VehicleDetailViewModel(
    private val authRepository: AuthRepository,
    private val getVehicleListUseCase: GetVehicleListUseCase,
    private val getVehicleMembersUseCase: GetVehicleMembersUseCase,
    private val createVehicleInviteUseCase: CreateVehicleInviteUseCase,
    private val removeVehicleMemberUseCase: RemoveVehicleMemberUseCase,
    private val transferVehicleOwnershipUseCase: TransferVehicleOwnershipUseCase,
    private val deleteVehicleUseCase: DeleteVehicleUseCase,
    private val removeUserFromVehicleUseCase: RemoveUserFromVehicleUseCase,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleDetailUiState())
    val uiState: StateFlow<VehicleDetailUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var membersJob: Job? = null

    /** Lo has borrado o te has salido tú desde aquí: que desaparezca no es «ya no tienes acceso». */
    private var leavingBySelf = false

    fun onEvent(event: VehicleDetailEvent) {
        when (event) {
            is VehicleDetailEvent.Load -> observe(event.vehicleId)
            VehicleDetailEvent.RetryMembers -> _uiState.value.vehicle?.let { loadMembers(it.id) }
            is VehicleDetailEvent.ActionRequested -> _uiState.update { it.copy(pendingAction = event.action) }
            VehicleDetailEvent.ActionDismissed -> _uiState.update { it.copy(pendingAction = null) }
            VehicleDetailEvent.ActionConfirmed -> confirmPending()
            VehicleDetailEvent.ShareClicked -> createInvite()
            VehicleDetailEvent.InviteDismissed -> _uiState.update { it.copy(invite = null) }
            VehicleDetailEvent.SnackBarDismissed -> _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observe(vehicleId: String) {
        // Empezar de cero: lo que quedara de otro vehículo —una invitación, una acción a medias— no
        // puede aparecer con el nombre de este.
        observeJob?.cancel()
        membersJob?.cancel()
        leavingBySelf = false
        _uiState.value = VehicleDetailUiState(vehicleId = vehicleId)

        observeJob = viewModelScope.launch {
            authRepository.authStateChanges
                .map { it?.uid }
                // El refresco del token, cada hora, también emite: sin esto se reabriría la consulta
                // para el mismo usuario. Ver `HomeViewModel`.
                .distinctUntilChanged()
                .flatMapLatest { uid ->
                    if (uid.isNullOrBlank()) {
                        // Sin sesión no hay nada que enseñar; la salida al login la lleva
                        // `SessionViewModel`.
                        flowOf(null to null)
                    } else {
                        getVehicleListUseCase(uid).map { vehicles -> uid to vehicles.firstOrNull { it.id == vehicleId } }
                    }
                }
                // Si la sesión terminó, no es un fallo: la salida la lleva `SessionViewModel` (spec 021).
                .catchListenerFailure({ authRepository.getCurrentUser() != null }) { error ->
                    crashReporter.recordFailure(LOAD_FAILED_CONTEXT, error.message)
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
                .collect { (uid, vehicle) -> onVehicle(uid, vehicle) }
        }
    }

    private fun onVehicle(uid: String?, vehicle: Vehicle?) {
        if (uid == null) return
        if (vehicle == null) {
            if (leavingBySelf) return
            membersJob?.cancel()
            _uiState.update { it.copy(isLoading = false, isGone = true, vehicle = null, pendingAction = null) }
            return
        }

        val previous = _uiState.value.vehicle
        _uiState.update { it.copy(isLoading = false, isGone = false, currentUserId = uid, vehicle = vehicle) }
        if (membersNeedRefresh(previous, vehicle)) loadMembers(vehicle.id)
    }

    private fun loadMembers(vehicleId: String) {
        membersJob?.cancel()
        // Si ya había lista se queda a la vista mientras llega la nueva: tras pasar la propiedad, la
        // lista no tiene por qué parpadear.
        if (_uiState.value.members !is MembersState.Loaded) {
            _uiState.update { it.copy(members = MembersState.Loading) }
        }
        membersJob = viewModelScope.launch {
            getVehicleMembersUseCase(vehicleId).fold(
                onSuccess = { members -> _uiState.update { it.copy(members = MembersState.Loaded(members)) } },
                onFailure = { error ->
                    // Ya no ser miembro lo cuenta la consulta del vehículo, que llegará enseguida.
                    if (error is NotAVehicleMemberException) return@fold
                    crashReporter.recordFailure(MEMBERS_FAILED_CONTEXT, error.message)
                    _uiState.update { it.copy(members = MembersState.Error) }
                }
            )
        }
    }

    private fun confirmPending() {
        val state = _uiState.value
        val action = state.pendingAction ?: return
        val vehicleId = state.vehicle?.id ?: return
        val uid = state.currentUserId ?: return
        _uiState.update { it.copy(pendingAction = null, isWorking = true) }

        if (action is DetailAction.DeleteVehicle || action is DetailAction.Leave) leavingBySelf = true

        viewModelScope.launch {
            val result = when (action) {
                is DetailAction.RemoveMember -> removeVehicleMemberUseCase(vehicleId, action.member.uid)
                is DetailAction.TransferTo -> transferVehicleOwnershipUseCase(vehicleId, action.member.uid)
                DetailAction.DeleteVehicle -> deleteVehicleUseCase(vehicleId, uid)
                DetailAction.Leave -> removeUserFromVehicleUseCase(vehicleId, uid)
            }
            result.fold(
                onSuccess = {
                    _uiState.update {
                        when (action) {
                            is DetailAction.RemoveMember ->
                                it.copy(isWorking = false, snackbarMessage = SnackbarMessage.Success(MEMBER_REMOVED_KEY))
                            is DetailAction.TransferTo ->
                                it.copy(isWorking = false, snackbarMessage = SnackbarMessage.Success(OWNER_TRANSFERRED_KEY))
                            DetailAction.DeleteVehicle, DetailAction.Leave -> it.copy(isWorking = false, closeRequested = true)
                        }
                    }
                },
                onFailure = { error ->
                    leavingBySelf = false
                    crashReporter.recordFailure(ACTION_FAILED_CONTEXT, error.message)
                    _uiState.update { it.copy(isWorking = false, snackbarMessage = SnackbarMessage.Error(ACTION_ERROR_KEY)) }
                }
            )
        }
    }

    private fun createInvite() {
        val vehicleId = _uiState.value.vehicle?.id ?: return
        _uiState.update { it.copy(isCreatingInvite = true) }

        viewModelScope.launch {
            createVehicleInviteUseCase(vehicleId).fold(
                onSuccess = { invite -> _uiState.update { it.copy(isCreatingInvite = false, invite = invite) } },
                onFailure = {
                    _uiState.update {
                        it.copy(isCreatingInvite = false, snackbarMessage = SnackbarMessage.Error(ERROR_INVITE_KEY))
                    }
                }
            )
        }
    }

    companion object {
        const val ERROR_INVITE_KEY = "share_vehicle_error"
        const val ACTION_ERROR_KEY = "delete_vehicle_error"
        const val MEMBER_REMOVED_KEY = "vehicle_detail_member_removed"
        const val OWNER_TRANSFERRED_KEY = "vehicle_detail_owner_transferred"
        private const val LOAD_FAILED_CONTEXT = "vehicle_detail_load_failed"
        private const val MEMBERS_FAILED_CONTEXT = "vehicle_members_failed"
        private const val ACTION_FAILED_CONTEXT = "vehicle_detail_action_failed"
    }
}

/**
 * Hay que volver a pedir los miembros cuando cambia quién está o quién manda. Lo demás —aparcar,
 * sobre todo— no los cambia, y no merece una llamada a la función.
 */
internal fun membersNeedRefresh(previous: Vehicle?, current: Vehicle): Boolean =
    previous == null || previous.memberIds != current.memberIds || previous.ownerId != current.ownerId

/** Una fila de la lista de miembros, con lo que la pantalla necesita para pintarla y decidir. */
data class MemberRow(val member: VehicleMember, val isOwner: Boolean, val isMe: Boolean)

/**
 * El dueño primero y el resto en el orden en que llegan. Quién es el dueño se toma del vehículo, que
 * es tiempo real, y no de la lista, que llega después: así, tras pasar la propiedad, la marca cambia
 * a la vez que las acciones y no una llamada más tarde.
 */
internal fun memberRows(members: List<VehicleMember>, ownerId: String, currentUserId: String?): List<MemberRow> {
    val rows = members.map { MemberRow(it, isOwner = it.uid == ownerId, isMe = it.uid == currentUserId) }
    return rows.filter { it.isOwner } + rows.filterNot { it.isOwner }
}

sealed interface MembersState {
    data object Loading : MembersState
    data object Error : MembersState
    data class Loaded(val members: List<VehicleMember>) : MembersState
}

/** Lo que espera confirmación. Solo una a la vez. */
sealed interface DetailAction {
    data class RemoveMember(val member: VehicleMember) : DetailAction
    data class TransferTo(val member: VehicleMember) : DetailAction
    data object DeleteVehicle : DetailAction
    data object Leave : DetailAction
}

data class VehicleDetailUiState(
    val vehicleId: String? = null,
    val vehicle: Vehicle? = null,
    val currentUserId: String? = null,
    val isLoading: Boolean = true,
    /** Estaba y ya no: te han quitado o lo han borrado. */
    val isGone: Boolean = false,
    val loadFailed: Boolean = false,
    val members: MembersState = MembersState.Loading,
    val pendingAction: DetailAction? = null,
    val isWorking: Boolean = false,
    /** Lo has borrado o te has salido desde aquí, y ha salido bien: la pantalla se cierra. */
    val closeRequested: Boolean = false,
    val isCreatingInvite: Boolean = false,
    val invite: VehicleInvite? = null,
    val snackbarMessage: SnackbarMessage? = null
) {
    val isOwner: Boolean get() = vehicle != null && vehicle.ownerId == currentUserId
    val vehicleName: String get() = vehicle?.name.orEmpty()
}

sealed class VehicleDetailEvent {
    data class Load(val vehicleId: String) : VehicleDetailEvent()
    data object RetryMembers : VehicleDetailEvent()
    data class ActionRequested(val action: DetailAction) : VehicleDetailEvent()
    data object ActionDismissed : VehicleDetailEvent()
    data object ActionConfirmed : VehicleDetailEvent()
    data object ShareClicked : VehicleDetailEvent()
    data object InviteDismissed : VehicleDetailEvent()
    data object SnackBarDismissed : VehicleDetailEvent()
}
