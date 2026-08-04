package com.albertomedina.apark.presentation.vehicledetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.model.VehicleInvite
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.CreateVehicleInviteUseCase
import com.albertomedina.apark.domain.usecase.GetVehicleByIdUseCase
import com.albertomedina.apark.utils.SnackbarMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Provisional detail screen: for now it only carries sharing. The full screen (editing, members)
 * comes in its own spec.
 */
class VehicleDetailViewModel(
    private val getVehicleByIdUseCase: GetVehicleByIdUseCase,
    private val createVehicleInviteUseCase: CreateVehicleInviteUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleDetailUiState())
    val uiState: StateFlow<VehicleDetailUiState> = _uiState.asStateFlow()

    fun onEvent(event: VehicleDetailEvent) {
        when (event) {
            is VehicleDetailEvent.Load -> load(event.vehicleId)
            is VehicleDetailEvent.ShareClicked -> createInvite()
            is VehicleDetailEvent.InviteDismissed -> _uiState.update { it.copy(invite = null) }
            is VehicleDetailEvent.SnackBarDismissed -> _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    private fun load(vehicleId: String) {
        _uiState.update { it.copy(isLoading = true, vehicleId = vehicleId) }

        viewModelScope.launch {
            val vehicle = getVehicleByIdUseCase(vehicleId)
            val currentUserId = authRepository.getCurrentUser()?.uid

            _uiState.update {
                it.copy(
                    isLoading = false,
                    vehicleName = vehicle?.name ?: "",
                    // Only the owner may share: the rules treat them differently, and the
                    // function rejects anyone else anyway.
                    isOwner = vehicle != null && vehicle.ownerId == currentUserId
                )
            }
        }
    }

    private fun createInvite() {
        val vehicleId = _uiState.value.vehicleId ?: return
        _uiState.update { it.copy(isCreatingInvite = true) }

        viewModelScope.launch {
            createVehicleInviteUseCase(vehicleId).fold(
                onSuccess = { invite ->
                    _uiState.update { it.copy(isCreatingInvite = false, invite = invite) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isCreatingInvite = false,
                            snackbarMessage = SnackbarMessage.Error(ERROR_INVITE_KEY)
                        )
                    }
                }
            )
        }
    }

    companion object {
        const val ERROR_INVITE_KEY = "share_vehicle_error"
    }
}

data class VehicleDetailUiState(
    val vehicleId: String? = null,
    val vehicleName: String = "",
    val isOwner: Boolean = false,
    val isLoading: Boolean = true,
    val isCreatingInvite: Boolean = false,
    val invite: VehicleInvite? = null,
    val snackbarMessage: SnackbarMessage? = null
)

sealed class VehicleDetailEvent {
    data class Load(val vehicleId: String) : VehicleDetailEvent()
    data object ShareClicked : VehicleDetailEvent()
    data object InviteDismissed : VehicleDetailEvent()
    data object SnackBarDismissed : VehicleDetailEvent()
}
