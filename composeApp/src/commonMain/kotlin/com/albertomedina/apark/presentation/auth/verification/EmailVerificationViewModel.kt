package com.albertomedina.apark.presentation.auth.verification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.model.User
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.repository.UserRepository
import com.albertomedina.apark.utils.SnackbarMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EmailVerificationViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailVerificationUiState())
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    init {
        val currentEmail = authRepository.getUserEmail() ?: ""
        _uiState.update { it.copy(email = currentEmail) }
    }

    fun onEvent(event: EmailVerificationEvent) {
        when (event) {
            is EmailVerificationEvent.CheckVerificationClicked -> checkEmailVerification()
            is EmailVerificationEvent.ResendEmailClicked -> resendVerificationEmail()
            is EmailVerificationEvent.BackToLoginClicked -> goBackToLogin()
            is EmailVerificationEvent.ErrorDismissed -> {
                _uiState.update { it.copy(snackbarMessage = null) }
            }

            is EmailVerificationEvent.NavigationHandled -> {
                _uiState.update {
                    it.copy(isVerificationComplete = false, shouldNavigateToLogin = false)
                }
            }
        }
    }

    private fun goBackToLogin() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(shouldNavigateToLogin = true) }
        }
    }

    private fun checkEmailVerification() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                user?.reload() // Actualiza el estado del usuario en Firebase

                val isVerified = user?.isEmailVerified == true
                _uiState.update { it.copy(isVerified = isVerified, isLoading = false) }

                // Si está verificado, automáticamente creamos el usuario en nuestra DB
                if (isVerified) {
                    createUserWhenVerified(user.uid, user.email ?: "")
                } else {
                    // Le avisamos de que aún no está verificado usando una llave para traducir en UI
                    _uiState.update {
                        it.copy(snackbarMessage = SnackbarMessage.Info("info_not_verified_yet"))
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snackbarMessage = SnackbarMessage.Error(e.message ?: "error_unknown")
                    )
                }
            }
        }
    }

    private fun resendVerificationEmail() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = authRepository.sendEmailVerification()

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            // Pasamos la llave de traducción
                            snackbarMessage = SnackbarMessage.Success("success_resend_email")
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = SnackbarMessage.Error(
                                exception.message ?: "error_unknown"
                            )
                        )
                    }
                }
            )
        }
    }

    private fun deleteAccount() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                authRepository.getCurrentUser()?.delete()
                // En lugar de llamar a una lambda, activamos el flag de navegación
                _uiState.update { it.copy(isLoading = false, shouldNavigateToLogin = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        snackbarMessage = SnackbarMessage.Error(e.message ?: "error_unknown")
                    )
                }
            }
        }
    }

    private suspend fun createUserWhenVerified(uid: String, email: String) {
        try {
            val user = User(id = uid, email = email)
            userRepository.createUser(user)
            _uiState.update { it.copy(isVerificationComplete = true) }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    snackbarMessage = SnackbarMessage.Error("error_creating_user_db")
                )
            }
        }
    }

}


data class EmailVerificationUiState(
    val email: String = "",
    val isVerified: Boolean = false,
    val isLoading: Boolean = false,
    val isVerificationComplete: Boolean = false,
    val shouldNavigateToLogin: Boolean = false,
    val snackbarMessage: SnackbarMessage? = null
)

sealed class EmailVerificationEvent {
    data object CheckVerificationClicked : EmailVerificationEvent()
    data object ResendEmailClicked : EmailVerificationEvent()
    data object BackToLoginClicked : EmailVerificationEvent()
    data object ErrorDismissed : EmailVerificationEvent()
    data object NavigationHandled : EmailVerificationEvent()
}