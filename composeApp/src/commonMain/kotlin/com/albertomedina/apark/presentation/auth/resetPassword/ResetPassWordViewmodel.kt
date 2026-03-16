package com.albertomedina.apark.presentation.auth.resetPassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.utils.SnackbarMessage
import com.albertomedina.apark.utils.validateEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResetPassWordViewmodel (
    private val authRepository: AuthRepository
): ViewModel()
{
    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun onEvent(event: ResetPasswordEvent){
        when(event){
            is ResetPasswordEvent.EmailChanged -> { _uiState.update{it.copy(email = event.email, emailError = validateEmail(event.email))}}
            ResetPasswordEvent.ResetPasswordClicked -> { performPasswordChange() }
            ResetPasswordEvent.ErrorDismissed -> { _uiState.update { it.copy(snackbarMessage = null) }}
            ResetPasswordEvent.NavigationHandled -> { _uiState.update { it.copy(emailSent = false) }}

        }
    }

    private fun performPasswordChange(){
        val emailErr = validateEmail(uiState.value.email)
        if (emailErr != null) {
            _uiState.update { it.copy(emailError = emailErr) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        val email = uiState.value.email

        viewModelScope.launch {
            val result = authRepository.resetPassword(email)

           result.fold(
               onSuccess = { 
                   _uiState.update { 
                       it.copy(
                           isLoading = false, 
                           emailSent = true, 
                           snackbarMessage = SnackbarMessage.Success("success_reset_password_sent") 
                       ) 
                   } 
               },
               onFailure = { error ->
                   val errorKey = when {
                       error.message?.contains("user-not-found", ignoreCase = true) == true -> "error_invalid_email"
                       else -> error.message ?: "error_unknown"
                   }
                   _uiState.update { 
                       it.copy(
                           isLoading = false, 
                           snackbarMessage = SnackbarMessage.Error(errorKey) 
                       ) 
                   }
               }
           )
        }
    }
}



data class ResetPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val emailSent: Boolean = false,
    val isLoading: Boolean = false,
    val snackbarMessage: SnackbarMessage? = null
)

sealed class ResetPasswordEvent{
    data class EmailChanged(var email: String): ResetPasswordEvent()
    data object ResetPasswordClicked: ResetPasswordEvent()
    data object ErrorDismissed: ResetPasswordEvent()
    data object NavigationHandled: ResetPasswordEvent()
}
