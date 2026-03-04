package com.albertomedina.apark.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.model.User
import com.albertomedina.apark.domain.repository.UserRepository
import com.albertomedina.apark.domain.usecase.LoginGoogleUseCase
import com.albertomedina.apark.domain.usecase.LoginUseCase
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val loginGoogleUseCase: LoginGoogleUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Regex simple para validar email en KMP (ya que Patterns es de Android)
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

            LoginEvent.ErrorDismissed -> {
                _uiState.update { it.copy(snackbarMessage = null) }
            }

            LoginEvent.OnNavigated -> {
                _uiState.update {
                    it.copy(shouldNavigate = false, shouldVerificate = false)
                }
            }
        }
    }

    private fun performLogin() {
        val email = _uiState.value.email
        val password = _uiState.value.password

        if (!isValidEmail(email)) {
            _uiState.update { it.copy(snackbarMessage = "Email inválido") } // Usar Res.string en el futuro
            return
        }

        if (password.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "La contraseña no puede estar vacía") }
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
                    val errorMsg =
                        if (error.message?.contains("Email_verification_required") == true) {
                            _uiState.update { it.copy(shouldVerificate = true) }
                            "Verifica tu email antes de entrar"
                        }else if (error.message?.contains("ERROR_INVALID_CREDENTIAL") == true) {
                            "Las credenciales son incorrectas. Verifica tu email y contraseña."
                        } else {
                            "Error: ${error.message}"
                        }
                    _uiState.update { it.copy(snackbarMessage = errorMsg) }
                }
            )
        }
    }

    private fun performGoogleLogin(idToken: String, accessToken: String?) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val result = loginGoogleUseCase(idToken, accessToken)

            result.fold(
                onSuccess = { firebaseUser ->
                    // Si es la primera vez, quizás queramos crear el usuario en nuestra DB
                    firebaseUser?.let { createUserInDbIfNecessary(it) }
                    _uiState.update { it.copy(isLoading = false, shouldNavigate = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snackbarMessage = "Error Google: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    private suspend fun createUserInDbIfNecessary(firebaseUser: FirebaseUser) {
        // Lógica opcional: crear documento de usuario si es login social
        val user = User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            name = firebaseUser.displayName ?: "",
            userVehicles = emptyList()
        )
        try {
            // Nota: createUser en el repo verifica si existe antes de sobrescribir
            userRepository.createUser(user)
        } catch (e: Exception) {
            println("Error creating user data: ${e.message}")
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(emailRegex)
    }
}

// LoginContract.kt (o dentro del mismo archivo ViewModel)

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val shouldNavigate: Boolean = false,
    val shouldVerificate: Boolean = false,
    val snackbarMessage: String? = null
)

sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    data object LoginClicked : LoginEvent()
    data class GoogleLoginClicked(val idToken: String, val accessToken: String? = null) : LoginEvent()
    data object ErrorDismissed : LoginEvent()
    data object OnNavigated : LoginEvent()
}