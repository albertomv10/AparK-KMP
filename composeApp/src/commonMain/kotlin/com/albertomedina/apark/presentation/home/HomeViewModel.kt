package com.albertomedina.apark.presentation.home

import androidx.compose.animation.core.copy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.DeleteVehicleUseCase
import com.albertomedina.apark.domain.usecase.GetVehicleListUseCase
import com.albertomedina.apark.domain.usecase.MoveVehicleUseCase
import com.albertomedina.apark.domain.usecase.RemoveUserFromVehicleUseCase
import com.albertomedina.apark.domain.usecase.SignOutUseCase
import com.albertomedina.apark.domain.usecase.UpdateVehicleLocationUseCase
import com.albertomedina.apark.utils.SnackbarMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val updateVehicleLocationUseCase: UpdateVehicleLocationUseCase,
    private val getVehicleListUseCase: GetVehicleListUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteVehicleUseCase: DeleteVehicleUseCase,
    private val removeUserFromVehicleUseCase: RemoveUserFromVehicleUseCase,
    private val moveVehicleUseCase: MoveVehicleUseCase,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authStateChanges
                .map { it?.uid }
                .flatMapLatest { userId ->
                    // Keep the uid around: the UI needs it to tell owner from shared member.
                    _uiState.update { it.copy(currentUserId = userId) }
                    // Ata los informes de fallo a un usuario — el uid, nunca el email — y los
                    // desata al cerrar sesión. Va aquí porque es donde el estado de auth ya fluye.
                    crashReporter.setUserId(userId)
                    if (!userId.isNullOrBlank()) {
                        getVehicleListUseCase(userId)
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { vehicleList ->
                    _uiState.update { it.copy(vehicles = vehicleList) }
                }
        }
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

            }

            is HomeEvent.AddVehicleClicked -> {

            }

            is HomeEvent.UndoLocationClicked -> {
                undoVehicleLocation(event.vehicleId, event.previousLocation)
            }

            is HomeEvent.OnMarkerDragged -> {
                saveManualLocation(event.vehicleId, event.latitude, event.longitude)
            }

            is HomeEvent.CenterMapOnUserClicked -> {
                _uiState.update { it.copy(centerCameraTrigger = it.centerCameraTrigger + 1) }
            }

            is HomeEvent.SignOutClicked -> {
                performSignOut()
            }

            is HomeEvent.NavigationHandled -> {
                _uiState.update { it.copy(shouldNavigateToLogin = false) }
            }

            is HomeEvent.SnackBarDismissed -> {
                _uiState.update {
                    it.copy(
                        locationUpdateSuccessData = null,
                        snackbarMessage = null
                    )
                }
            }
            is HomeEvent.OpenSettingsClicked -> {
                _uiState.update { it.copy(openSettingsTrigger = it.openSettingsTrigger + 1) }
            }

            is HomeEvent.PermisionsDenied -> {
                _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_gps_permissions")) }
            }

            is HomeEvent.VehicleLongPressed -> {
                _uiState.update { it.copy(isEditMode = true) }
            }

            is HomeEvent.EditModeExited -> {
                _uiState.update { it.copy(isEditMode = false, followedVehicleId = null) }
            }

            is HomeEvent.DeleteVehicleClicked -> {
                askDeleteConfirmation(event.vehicleId)
            }

            is HomeEvent.DeleteConfirmed -> {
                confirmDeletion()
            }

            is HomeEvent.DeleteDismissed -> {
                _uiState.update { it.copy(pendingDeletion = null) }
            }

            is HomeEvent.MoveVehicleClicked -> {
                moveVehicle(event.vehicleId, event.offset)
            }

        }
    }

    private fun updateVehicleLocation(vehicleId: String) {
        val vehicle = _uiState.value.vehicles.find { it.id == vehicleId }
        val previousLocation = vehicle?.lastLocation
        _uiState.update { it.copy(isLoading = true, updatingVehicleId = vehicleId) }

        viewModelScope.launch {
            try {
                val result = updateVehicleLocationUseCase(vehicleId)

                result.fold(
                    onSuccess = {
                        if (previousLocation != null) {
                            _uiState.update { state ->
                                state.copy(

                                    locationUpdateSuccessData = UndoLocationData(
                                        vehicleId,
                                        previousLocation
                                    ),
                                    snackbarMessage = SnackbarMessage.Success("success_location_updated")
                                )
                            }
                        }
                    },
                    onFailure = {
                        _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_location_save")) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_gps_permissions")) }
            } finally {
                _uiState.update { it.copy(isLoading = false, updatingVehicleId = null) }
            }
        }
    }

    private fun undoVehicleLocation(vehicleId: String, previousLocation: Vehicle.LocationModel) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = updateVehicleLocationUseCase(vehicleId, previousLocation, true)

            result.fold(
                onSuccess = {

                },
                onFailure = {
                    _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_undo_failed")) }
                }
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun saveManualLocation(vehicleId: String, latitude: Double, longitude: Double) {
        _uiState.update { it.copy(isLoading = true, updatingVehicleId = vehicleId) }
        val vehicle = _uiState.value.vehicles.find { it.id == vehicleId }
        val previousLocation = vehicle?.lastLocation
        viewModelScope.launch {
            val result = updateVehicleLocationUseCase(
                vehicleId = vehicleId,
                manualLocation = Vehicle.LocationModel(
                    latitude = latitude,
                    longitude = longitude
                )
            )
            result.fold(
                onSuccess = {
                    if (previousLocation != null) {
                        _uiState.update { state ->
                            state.copy(
                                locationUpdateSuccessData = UndoLocationData(
                                    vehicleId,
                                    previousLocation
                                ),
                                snackbarMessage = SnackbarMessage.Success("success_location_updated")
                            )
                        }
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_location_save")) }
                }
            )

            _uiState.update { it.copy(isLoading = false, updatingVehicleId = null) }
        }
    }

    private fun askDeleteConfirmation(vehicleId: String) {
        val vehicle = _uiState.value.vehicles.find { it.id == vehicleId } ?: return
        val currentUserId = _uiState.value.currentUserId

        if (currentUserId == null) {
            _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error(ERROR_NOT_AUTHENTICATED_KEY)) }
            return
        }

        _uiState.update {
            it.copy(
                pendingDeletion = PendingDeletion(
                    vehicleId = vehicleId,
                    vehicleName = vehicle.name,
                    isOwner = vehicle.ownerId == currentUserId
                )
            )
        }
    }

    private fun confirmDeletion() {
        val pending = _uiState.value.pendingDeletion ?: return
        val userId = _uiState.value.currentUserId ?: return

        _uiState.update {
            it.copy(isLoading = true, updatingVehicleId = pending.vehicleId, pendingDeletion = null)
        }

        viewModelScope.launch {
            try {
                // Owner deletes the vehicle for everyone; a shared member only drops their own
                // membership, leaving the vehicle intact for the rest.
                val result = if (pending.isOwner) {
                    deleteVehicleUseCase(pending.vehicleId, userId)
                } else {
                    removeUserFromVehicleUseCase(pending.vehicleId, userId)
                }

                result.fold(
                    onSuccess = {
                        val successKey = if (pending.isOwner) SUCCESS_DELETED_KEY else SUCCESS_REMOVED_KEY
                        _uiState.update {
                            it.copy(
                                isEditMode = false,
                                snackbarMessage = SnackbarMessage.Success(successKey)
                            )
                        }
                    },
                    onFailure = {
                        _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error(ERROR_DELETE_KEY)) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error(ERROR_DELETE_KEY)) }
            } finally {
                _uiState.update { it.copy(isLoading = false, updatingVehicleId = null) }
            }
        }
    }

    private fun moveVehicle(vehicleId: String, offset: Int) {
        val userId = _uiState.value.currentUserId ?: return
        val vehicles = _uiState.value.vehicles
        val from = vehicles.indexOfFirst { it.id == vehicleId }
        val to = from + offset

        // The arrows are disabled at the ends, but guard anyway: the list may have changed
        // underneath between composition and the tap.
        if (from == -1 || to !in vehicles.indices) return

        viewModelScope.launch {
            moveVehicleUseCase(userId, vehicleId, offset).fold(
                onSuccess = {
                    // Follow the moved card, so tapping the same arrow keeps moving it. Only
                    // the id is published: where it landed is whatever the reordered list says.
                    _uiState.update { it.copy(followedVehicleId = vehicleId) }
                },
                onFailure = {
                    _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error(ERROR_REORDER_KEY)) }
                }
            )
        }
    }

    private fun performSignOut(){
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            signOutUseCase().fold(
                onSuccess = { _uiState.update { it.copy(isLoading = false, shouldNavigateToLogin = true) } },
                onFailure = { _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_sing_out")) } }
            )
        }
    }

    companion object {
        const val SUCCESS_DELETED_KEY = "delete_vehicle_success_deleted"
        const val SUCCESS_REMOVED_KEY = "delete_vehicle_success_removed"
        const val ERROR_DELETE_KEY = "delete_vehicle_error"
        const val ERROR_NOT_AUTHENTICATED_KEY = "delete_vehicle_error_not_authenticated"
        const val ERROR_REORDER_KEY = "reorder_error"
    }
}

data class HomeUiState(
    val userEmail: String = "",
    val currentUserId: String? = null,
    val vehicles: List<Vehicle> = emptyList(),
    val selectedVehicleIndex: Int = 0,
    val isLoading: Boolean = false,
    val updatingVehicleId: String? = null,
    val centerCameraTrigger: Int = 0,
    val openSettingsTrigger: Int = 0,
    val shouldNavigateToLogin: Boolean = false,
    val locationUpdateSuccessData: UndoLocationData? = null,
    val snackbarMessage: SnackbarMessage? = null,
    val isEditMode: Boolean = false,
    val pendingDeletion: PendingDeletion? = null,
    /**
     * Vehicle the carousel keeps centred while reordering, held until edit mode ends rather
     * than consumed on the first scroll: the reordered list can arrive after the scroll would
     * run, and a one-shot flag gets cleared against the old order, leaving the pager behind.
     */
    val followedVehicleId: String? = null
)

/** A delete awaiting user confirmation. [isOwner] decides which action (and copy) applies. */
data class PendingDeletion(
    val vehicleId: String,
    val vehicleName: String,
    val isOwner: Boolean
)

data class UndoLocationData(
    val vehicleId: String,
    val previousLocation: Vehicle.LocationModel
)

sealed class HomeEvent {
    data class OnVehicleSwiped(val newIndex: Int) : HomeEvent()
    data class UpdateLocationClicked(val vehicleId: String) : HomeEvent()
    data class VehicleDetailsClicked(val vehicleId: String) : HomeEvent()
    data class UndoLocationClicked(val vehicleId:String, val previousLocation: Vehicle.LocationModel): HomeEvent()
    data class OnMarkerDragged(
        val vehicleId: String,
        val latitude: Double,
        val longitude: Double
    ) : HomeEvent()
    data object PermisionsDenied : HomeEvent()
    data object OpenSettingsClicked : HomeEvent()
    data object CenterMapOnUserClicked : HomeEvent()
    data object AddVehicleClicked : HomeEvent()
    data object SignOutClicked : HomeEvent()
    data object NavigationHandled : HomeEvent()
    data object SnackBarDismissed : HomeEvent()
    data object VehicleLongPressed : HomeEvent()
    data object EditModeExited : HomeEvent()
    data class DeleteVehicleClicked(val vehicleId: String) : HomeEvent()
    data object DeleteConfirmed : HomeEvent()
    data object DeleteDismissed : HomeEvent()
    data class MoveVehicleClicked(val vehicleId: String, val offset: Int) : HomeEvent()
}
