package com.albertomedina.apark.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.auth.AuthFailure
import com.albertomedina.apark.domain.auth.AuthFailureReason
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.ChangeEmailUseCase
import com.albertomedina.apark.utils.validateEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChangeEmailViewModel(
    authRepository: AuthRepository,
    private val changeEmailUseCase: ChangeEmailUseCase,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val currentEmail = authRepository.getUserEmail().orEmpty()

    private val _uiState = MutableStateFlow(ChangeEmailUiState())
    val uiState: StateFlow<ChangeEmailUiState> = _uiState.asStateFlow()

    fun onEvent(event: ChangeEmailEvent) {
        when (event) {
            is ChangeEmailEvent.EmailChanged -> _uiState.update {
                it.copy(newEmail = event.email, emailError = null, generalError = null)
            }
            is ChangeEmailEvent.PasswordChanged -> _uiState.update {
                it.copy(password = event.password, passwordError = null, generalError = null)
            }
            ChangeEmailEvent.SubmitClicked -> submit()
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val email = state.newEmail.trim()

        val emailError = validateEmail(email)
            ?: "error_same_email".takeIf { email.equals(currentEmail, ignoreCase = true) }
        val passwordError = "error_empty_password".takeIf { state.password.isEmpty() }
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, generalError = null) }
        viewModelScope.launch {
            changeEmailUseCase(email, state.password).fold(
                onSuccess = { _uiState.update { it.copy(isSubmitting = false, sentTo = email) } },
                onFailure = { error -> onFailed(error) }
            )
        }
    }

    /**
     * Lo que es de un campo se enseña en ese campo, y **los dos conservan lo escrito**: un error de
     * la contraseña no obliga a volver a teclear el email (criterio 4).
     */
    private fun onFailed(error: Throwable) {
        val reason = (error as? AuthFailure)?.reason
        _uiState.update {
            when (reason) {
                AuthFailureReason.INVALID_CREDENTIALS ->
                    it.copy(isSubmitting = false, passwordError = "error_wrong_current_password")
                AuthFailureReason.EMAIL_ALREADY_IN_USE ->
                    it.copy(isSubmitting = false, emailError = "error_email_in_use")
                AuthFailureReason.INVALID_EMAIL ->
                    it.copy(isSubmitting = false, emailError = "error_invalid_email")
                else -> it.copy(
                    isSubmitting = false,
                    generalError = error.generalAccountErrorKey(crashReporter, CHANGE_EMAIL_CONTEXT)
                )
            }
        }
    }

    companion object {
        const val CHANGE_EMAIL_CONTEXT = "change_email_failed"
    }
}

data class ChangeEmailUiState(
    val newEmail: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isSubmitting: Boolean = false,
    /** A dónde se mandó el enlace. Mientras no sea null, la pantalla enseña la confirmación. */
    val sentTo: String? = null
)

sealed class ChangeEmailEvent {
    data class EmailChanged(val email: String) : ChangeEmailEvent()
    data class PasswordChanged(val password: String) : ChangeEmailEvent()
    data object SubmitClicked : ChangeEmailEvent()
}
