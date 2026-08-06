package com.albertomedina.apark.presentation.addvehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.model.JoinStatus
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.CreateVehicleUseCase
import com.albertomedina.apark.domain.usecase.JoinVehicleWithCodeUseCase
import com.albertomedina.apark.utils.SnackbarMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddVehicleViewModel(
    private val createVehicleUseCase: CreateVehicleUseCase,
    private val joinVehicleWithCodeUseCase: JoinVehicleWithCodeUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddVehicleUiState())
    val uiState: StateFlow<AddVehicleUiState> = _uiState.asStateFlow()

    fun onEvent(event: AddVehicleEvent) {
        when (event) {
            is AddVehicleEvent.NameChanged -> {
                _uiState.update { it.copy(name = event.name, nameError = false) }
            }
            is AddVehicleEvent.LicensePlateChanged -> {
                _uiState.update { it.copy(licensePlate = event.licensePlate) }
            }
            AddVehicleEvent.SaveClicked -> save()
            is AddVehicleEvent.TabSelected -> {
                _uiState.update { it.copy(selectedTab = event.index) }
            }
            is AddVehicleEvent.CodeChanged -> {
                _uiState.update { it.copy(code = event.code, codeError = false) }
            }
            AddVehicleEvent.JoinClicked -> join()
            AddVehicleEvent.SnackBarDismissed -> {
                _uiState.update { it.copy(snackbarMessage = null) }
            }
            AddVehicleEvent.NavigationHandled -> {
                _uiState.update { it.copy(shouldNavigateBack = false) }
            }
            AddVehicleEvent.ScreenOpened -> {
                // NavDisplay does not scope ViewModels to the navigation entry, so this one
                // survives leaving the screen. Without wiping the state, a new visit would find
                // the previous visit's text still typed in.
                _uiState.value = AddVehicleUiState()
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        val name = state.name.trim()

        if (name.isBlank()) {
            _uiState.update { it.copy(nameError = true) }
            return
        }

        val userId = authRepository.getCurrentUser()?.uid
        if (userId == null) {
            _uiState.update {
                it.copy(snackbarMessage = SnackbarMessage.Error(ERROR_NOT_AUTHENTICATED_KEY))
            }
            return
        }

        val licensePlate = state.licensePlate.trim().uppercase()

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val result = createVehicleUseCase(userId, name, licensePlate)
            _uiState.update { it.copy(isLoading = false) }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(shouldNavigateBack = true) }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(snackbarMessage = SnackbarMessage.Error(ERROR_GENERIC_KEY))
                    }
                }
            )
        }
    }

    private fun join() {
        val code = _uiState.value.code.trim()

        if (code.isBlank()) {
            _uiState.update { it.copy(codeError = true) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val result = joinVehicleWithCodeUseCase(code)
            _uiState.update { it.copy(isLoading = false) }

            result.fold(
                onSuccess = { joinResult ->
                    if (joinResult.status == JoinStatus.OK) {
                        _uiState.update {
                            it.copy(joinedVehicleName = joinResult.vehicleName, shouldNavigateBack = true)
                        }
                    } else {
                        // Each outcome gets its own wording; the raw platform exception must never
                        // reach the user.
                        val key = when (joinResult.status) {
                            JoinStatus.INVALID -> ERROR_JOIN_INVALID_KEY
                            JoinStatus.USED -> ERROR_JOIN_USED_KEY
                            JoinStatus.EXPIRED -> ERROR_JOIN_EXPIRED_KEY
                            JoinStatus.ALREADY_MEMBER -> ERROR_JOIN_ALREADY_MEMBER_KEY
                            else -> ERROR_JOIN_KEY
                        }
                        _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error(key)) }
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error(ERROR_JOIN_KEY)) }
                }
            )
        }
    }

    companion object {
        const val ERROR_JOIN_KEY = "join_vehicle_error"
        const val ERROR_JOIN_INVALID_KEY = "join_vehicle_error_invalid"
        const val ERROR_JOIN_USED_KEY = "join_vehicle_error_used"
        const val ERROR_JOIN_EXPIRED_KEY = "join_vehicle_error_expired"
        const val ERROR_JOIN_ALREADY_MEMBER_KEY = "join_vehicle_error_already_member"
        const val ERROR_NOT_AUTHENTICATED_KEY = "add_vehicle_error_not_authenticated"
        const val ERROR_GENERIC_KEY = "add_vehicle_error_generic"
    }
}

data class AddVehicleUiState(
    val name: String = "",
    val nameError: Boolean = false,
    val licensePlate: String = "",
    val isLoading: Boolean = false,
    val shouldNavigateBack: Boolean = false,
    val snackbarMessage: SnackbarMessage? = null,
    val selectedTab: Int = 0,
    val code: String = "",
    val codeError: Boolean = false,
    val joinedVehicleName: String? = null
)

sealed class AddVehicleEvent {
    data class NameChanged(val name: String) : AddVehicleEvent()
    data class LicensePlateChanged(val licensePlate: String) : AddVehicleEvent()
    data object SaveClicked : AddVehicleEvent()
    data object ScreenOpened : AddVehicleEvent()
    data object SnackBarDismissed : AddVehicleEvent()
    data object NavigationHandled : AddVehicleEvent()
    data class TabSelected(val index: Int) : AddVehicleEvent()
    data class CodeChanged(val code: String) : AddVehicleEvent()
    data object JoinClicked : AddVehicleEvent()
}
