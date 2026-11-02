package com.albertomedina.apark.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.auth.AuthFailure
import com.albertomedina.apark.domain.auth.AuthFailureReason
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.usecase.ChangePasswordUseCase
import com.albertomedina.apark.utils.validateConfirmPassword
import com.albertomedina.apark.utils.validatePassword
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChangePasswordViewModel(
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun onEvent(event: ChangePasswordEvent) {
        when (event) {
            is ChangePasswordEvent.CurrentChanged -> _uiState.update {
                it.copy(current = event.password, currentError = null, generalError = null)
            }
            is ChangePasswordEvent.NewChanged -> _uiState.update {
                it.copy(new = event.password, newError = null, generalError = null)
            }
            is ChangePasswordEvent.ConfirmChanged -> _uiState.update {
                it.copy(confirm = event.password, confirmError = null, generalError = null)
            }
            ChangePasswordEvent.SubmitClicked -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return

        // Las mismas reglas que al registrarse: la contraseña nueva no puede ser más floja que la
        // que se pidió al crear la cuenta.
        val currentError = "error_empty_password".takeIf { state.current.isEmpty() }
        val newError = validatePassword(state.new)
            ?: "error_same_password".takeIf { state.new == state.current }
        val confirmError = validateConfirmPassword(state.new, state.confirm)
        if (currentError != null || newError != null || confirmError != null) {
            _uiState.update { it.copy(currentError = currentError, newError = newError, confirmError = confirmError) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, generalError = null) }
        viewModelScope.launch {
            changePasswordUseCase(state.current, state.new).fold(
                onSuccess = { _uiState.update { ChangePasswordUiState(done = true) } },
                onFailure = { error -> onFailed(error) }
            )
        }
    }

    private fun onFailed(error: Throwable) {
        val reason = (error as? AuthFailure)?.reason
        _uiState.update {
            when (reason) {
                AuthFailureReason.INVALID_CREDENTIALS ->
                    it.copy(isSubmitting = false, currentError = "error_wrong_current_password")
                AuthFailureReason.WEAK_PASSWORD ->
                    it.copy(isSubmitting = false, newError = "error_weak_password")
                else -> it.copy(
                    isSubmitting = false,
                    generalError = error.generalAccountErrorKey(crashReporter, CHANGE_PASSWORD_CONTEXT)
                )
            }
        }
    }

    companion object {
        const val CHANGE_PASSWORD_CONTEXT = "change_password_failed"
    }
}

data class ChangePasswordUiState(
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val currentError: String? = null,
    val newError: String? = null,
    val confirmError: String? = null,
    val generalError: String? = null,
    val isSubmitting: Boolean = false,
    /** Cambiada. Se vacían los campos: no hay motivo para que una contraseña siga en memoria. */
    val done: Boolean = false
)

sealed class ChangePasswordEvent {
    data class CurrentChanged(val password: String) : ChangePasswordEvent()
    data class NewChanged(val password: String) : ChangePasswordEvent()
    data class ConfirmChanged(val password: String) : ChangePasswordEvent()
    data object SubmitClicked : ChangePasswordEvent()
}
