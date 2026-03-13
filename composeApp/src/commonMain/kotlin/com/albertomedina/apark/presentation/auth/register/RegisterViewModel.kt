package com.albertomedina.apark.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.usecase.RegisterUseCase
import com.albertomedina.apark.utils.SnackbarMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.EmailChanged -> {
                _uiState.update { it.copy(email = event.email, emailError = validateEmail(event.email)) }
            }
            is RegisterEvent.PasswordChanged -> {
                _uiState.update { 
                    it.copy(
                        password = event.password, 
                        passwordError = validatePassword(event.password),
                        confirmPasswordError = validateConfirmPassword(event.password, it.confirmPassword)
                    ) 
                }
            }
            is RegisterEvent.ConfirmPasswordChanged -> {
                _uiState.update { 
                    it.copy(
                        confirmPassword = event.confirmPassword, 
                        confirmPasswordError = validateConfirmPassword(it.password, event.confirmPassword)
                    ) 
                }
            }
            RegisterEvent.RegisterClicked -> {
                performRegister()
            }
            RegisterEvent.ErrorDismissed -> {
                _uiState.update { it.copy(snackbarMessage = null) }
            }
            RegisterEvent.OnNavigated -> {
                _uiState.update { it.copy(shouldNavigateToVerify = false) }
            }
        }
    }

    private fun performRegister() {
        val state = _uiState.value
        
        // Validaciones finales antes de disparar
        val emailErr = validateEmail(state.email)
        val passErr = validatePassword(state.password)
        val confPassErr = validateConfirmPassword(state.password, state.confirmPassword)

        if (emailErr != null || passErr != null || confPassErr != null) {
            _uiState.update { it.copy(emailError = emailErr, passwordError = passErr, confirmPasswordError = confPassErr) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val result = registerUseCase(state.email, state.password)
            _uiState.update { it.copy(isLoading = false) }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(shouldNavigateToVerify = true) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error(error.message ?: "Unknown Error")) }
                }
            )
        }
    }

    private fun validateEmail(email: String): String? {
        if (email.isBlank()) return "error_empty_email"
        if (!email.matches(emailRegex)) return "error_invalid_email"
        return null
    }

    private fun validatePassword(password: String): String? {
        if (password.isBlank()) return "error_empty_password"
        if (password.length < 6) return "error_password_too_short"
        if (password.none { it.isUpperCase() }) return "error_password_no_uppercase"
        if (password.none { it.isDigit() }) return "error_password_no_number"
        return null
    }

    private fun validateConfirmPassword(password: String, confirm: String): String? {
        if (confirm.isBlank()) return "error_empty_password"
        if (password != confirm) return "error_passwords_not_match"
        return null
    }
}

data class RegisterUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val shouldNavigateToVerify: Boolean = false,
    val snackbarMessage: SnackbarMessage? = null
)

sealed class RegisterEvent {
    data class EmailChanged(val email: String) : RegisterEvent()
    data class PasswordChanged(val password: String) : RegisterEvent()
    data class ConfirmPasswordChanged(val confirmPassword: String) : RegisterEvent()
    data object RegisterClicked : RegisterEvent()
    data object ErrorDismissed : RegisterEvent()
    data object OnNavigated : RegisterEvent()
}
