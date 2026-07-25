package com.albertomedina.apark.presentation.addvehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.CreateVehicleUseCase
import com.albertomedina.apark.utils.SnackbarMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddVehicleViewModel(
    private val createVehicleUseCase: CreateVehicleUseCase,
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
            AddVehicleEvent.SnackBarDismissed -> {
                _uiState.update { it.copy(snackbarMessage = null) }
            }
            AddVehicleEvent.NavigationHandled -> {
                _uiState.update { it.copy(shouldNavigateBack = false) }
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

    companion object {
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
    val snackbarMessage: SnackbarMessage? = null
)

sealed class AddVehicleEvent {
    data class NameChanged(val name: String) : AddVehicleEvent()
    data class LicensePlateChanged(val licensePlate: String) : AddVehicleEvent()
    data object SaveClicked : AddVehicleEvent()
    data object SnackBarDismissed : AddVehicleEvent()
    data object NavigationHandled : AddVehicleEvent()
}
