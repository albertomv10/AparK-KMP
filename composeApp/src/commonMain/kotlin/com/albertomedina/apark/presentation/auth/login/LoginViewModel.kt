package com.albertomedina.apark.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.model.User
import com.albertomedina.apark.domain.repository.UserRepository
import com.albertomedina.apark.domain.usecase.LoginAppleUseCase
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.usecase.LoginGoogleUseCase
import com.albertomedina.apark.domain.usecase.LoginUseCase
import com.albertomedina.apark.presentation.components.SocialLoginFailure
import com.albertomedina.apark.presentation.components.SocialLoginReason
import com.albertomedina.apark.utils.SnackbarMessage
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val loginGoogleUseCase: LoginGoogleUseCase,
    private val loginAppleUseCase: LoginAppleUseCase,
    private val userRepository: UserRepository,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                _uiState.update { it.copy(email = event.email) }
            }

            is LoginEvent.PasswordChanged -> {
                _uiState.update { it.copy(password = event.password) }
            }

            LoginEvent.LoginClicked -> {
                performLogin()
            }

            is LoginEvent.GoogleLoginClicked -> {
                performGoogleLogin(event.idToken, event.accessToken)
            }

            is LoginEvent.SocialLoginFailed -> {
                onSocialLoginFailed(event.failure)
            }

            is LoginEvent.AppleLoginClicked -> {
                performAppleLogin(event.idToken, event.nonce)
            }

            LoginEvent.ResetPasswordClicked -> {
                _uiState.update { it.copy(shouldResetPassword = true) }
            }
            LoginEvent.RegisterClicked -> {
                _uiState.update { it.copy(shouldRegister = true) }
            }

            LoginEvent.ErrorDismissed -> {
                _uiState.update { it.copy(snackbarMessage = null) }
            }

            LoginEvent.OnNavigated -> {
                _uiState.update {
                    it.copy(shouldNavigate = false, shouldVerificate = false, shouldResetPassword = false, shouldRegister = false)
                }
            }
        }
    }

    private fun performLogin() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        if (!isValidEmail(email)) {
            _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_invalid_email")) }
            return
        }

        if (password.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error("error_empty_password")) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val result = loginUseCase(email, password)

            _uiState.update { state ->
                state.copy(isLoading = false)
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(shouldNavigate = true) }
                },
                onFailure = { error ->
                    val errorKey =
                        if (error.message?.contains("Email_verification_required") == true) {
                            _uiState.update { it.copy(shouldVerificate = true) }
                            "error_verify_email"
                        } else if (error.message?.contains("credential", ignoreCase = true) == true) {
                            "error_invalid_credentials"
                        } else {
                            error.message ?: "Unknown error"
                        }
                    _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error(errorKey)) }
                }
            )
        }
    }

    /**
     * Cancelar es una decisión del usuario, no un fallo: no se avisa de nada. El resto se traduce
     * a un mensaje que se pueda leer; el detalle técnico no se enseña — irá a Crashlytics.
     */
    private fun onSocialLoginFailed(failure: SocialLoginFailure) {
        if (failure.reason == SocialLoginReason.CANCELLED) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        // El detalle técnico se deja de enseñar al usuario, pero no se pierde: este es el sitio
        // que no existía cuando un `[28444]` de OAuth costó media tarde de adb.
        crashReporter.recordFailure(SOCIAL_LOGIN_CONTEXT, failure.detail)

        val key = when (failure.reason) {
            SocialLoginReason.NO_ACCOUNTS -> ERROR_NO_ACCOUNTS_KEY
            else -> ERROR_GOOGLE_KEY
        }
        _uiState.update {
            it.copy(isLoading = false, snackbarMessage = SnackbarMessage.Error(key))
        }
    }

    private fun performGoogleLogin(idToken: String, accessToken: String?) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val result = loginGoogleUseCase(idToken, accessToken)

            result.fold(
                onSuccess = { firebaseUser ->
                    firebaseUser?.let { createUserInDbIfNecessary(it) }
                    _uiState.update { it.copy(isLoading = false, shouldNavigate = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = SnackbarMessage.Error(ERROR_GOOGLE_KEY)
                        )
                    }
                }
            )
        }
    }

    private fun performAppleLogin(idToken: String, nonce: String) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val result = loginAppleUseCase(idToken, nonce)

            result.fold(
                onSuccess = { firebaseUser ->
                    firebaseUser?.let { createUserInDbIfNecessary(it) }
                    _uiState.update { it.copy(isLoading = false, shouldNavigate = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = SnackbarMessage.Error("error_apple_login")
                        )
                    }
                }
            )
        }
    }

    private suspend fun createUserInDbIfNecessary(firebaseUser: FirebaseUser) {
        val user = User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            name = firebaseUser.displayName ?: "",
            userVehicles = emptyList()
        )
        try {
            userRepository.createUser(user)
        } catch (e: Exception) {
            println("Error creating user data: ${e.message}")
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(emailRegex)
    }

    companion object {
        const val SOCIAL_LOGIN_CONTEXT = "social_login_failed"
        const val ERROR_GOOGLE_KEY = "error_google_login"
        const val ERROR_NO_ACCOUNTS_KEY = "error_social_no_accounts"
    }
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val shouldNavigate: Boolean = false,
    val shouldVerificate: Boolean = false,
    val shouldResetPassword: Boolean = false,
    val shouldRegister: Boolean = false,
    val snackbarMessage: SnackbarMessage? = null
)

sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    data object LoginClicked : LoginEvent()
    data object ResetPasswordClicked : LoginEvent()
    data object RegisterClicked : LoginEvent()
    data class GoogleLoginClicked(val idToken: String, val accessToken: String? = null) : LoginEvent()
    data class AppleLoginClicked(val idToken: String, val nonce: String) : LoginEvent()
    /** Un botón de login social falló antes de llegar a Firebase (Credential Manager, Apple ID…). */
    data class SocialLoginFailed(val failure: SocialLoginFailure) : LoginEvent()
    data object ErrorDismissed : LoginEvent()
    data object OnNavigated : LoginEvent()
}
