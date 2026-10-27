package com.albertomedina.apark.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.auth.AuthFailure
import com.albertomedina.apark.domain.auth.AuthFailureReason
import com.albertomedina.apark.domain.auth.SignInProvider
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.settings.SettingsRepository
import com.albertomedina.apark.domain.settings.ThemeMode
import com.albertomedina.apark.domain.usecase.GetUserUseCase
import com.albertomedina.apark.domain.usecase.InvalidUserNameException
import com.albertomedina.apark.domain.usecase.SignOutUseCase
import com.albertomedina.apark.domain.usecase.SyncUserProfileUseCase
import com.albertomedina.apark.domain.usecase.USER_NAME_MAX_LENGTH
import com.albertomedina.apark.domain.usecase.UpdateUserNameUseCase
import com.albertomedina.apark.domain.usecase.UserNameError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val authRepository: AuthRepository,
    private val getUserUseCase: GetUserUseCase,
    private val updateUserNameUseCase: UpdateUserNameUseCase,
    private val syncUserProfileUseCase: SyncUserProfileUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        refreshAccount()
        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
        }
        authRepository.getCurrentUser()?.uid?.let { uid ->
            viewModelScope.launch {
                getUserUseCase(uid)
                    // Al cerrar sesión el listener recibe PERMISSION_DENIED antes de que la pantalla
                    // se vaya. No es un fallo que contar: es la sesión terminando.
                    .catch { }
                    .collect { user -> _uiState.update { it.copy(name = user.name) } }
            }
        }
        viewModelScope.launch {
            // Pregunta al servidor si la sesión sigue valiendo. Si el email se cambió desde el
            // enlace, ya no vale, y el repositorio la cierra: la pantalla no llega a enseñar el
            // email de antes. Cualquier otro fallo —sin red— se ignora: nadie sale de la app por
            // estar sin cobertura.
            authRepository.reloadUser().onSuccess {
                // Si el email cambió y la sesión siguió viva, el documento se pone al día aquí
                // y no en la próxima entrada: es lo que hace que las tarjetas lo reflejen.
                syncUserProfileUseCase()
            }
            refreshAccount()
        }
    }

    private fun refreshAccount() {
        _uiState.update {
            it.copy(email = authRepository.getUserEmail().orEmpty(), provider = authRepository.signInProvider())
        }
    }

    fun onEvent(event: AccountEvent) {
        when (event) {
            is AccountEvent.ThemeSelected -> viewModelScope.launch {
                settingsRepository.setThemeMode(event.mode)
            }

            // No navega: cerrar sesión hace que `authStateChanges` emita null, y es la sesión de las
            // pestañas la que lleva al login, igual que cuando la sesión termina por otra causa.
            is AccountEvent.SignOutClicked -> performSignOut()

            AccountEvent.EditNameClicked -> _uiState.update {
                it.copy(nameDialog = NameDialogState(draft = it.name))
            }

            is AccountEvent.NameDraftChanged -> _uiState.update {
                it.copy(nameDialog = it.nameDialog?.copy(draft = event.name.take(USER_NAME_MAX_LENGTH), errorKey = null))
            }

            AccountEvent.NameSaveClicked -> saveName()

            AccountEvent.NameDialogDismissed -> _uiState.update { it.copy(nameDialog = null) }
        }
    }

    private fun saveName() {
        val dialog = _uiState.value.nameDialog ?: return
        _uiState.update { it.copy(nameDialog = dialog.copy(isSaving = true, errorKey = null)) }
        viewModelScope.launch {
            updateUserNameUseCase(dialog.draft).fold(
                onSuccess = { _uiState.update { it.copy(nameDialog = null) } },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(nameDialog = it.nameDialog?.copy(isSaving = false, errorKey = error.toNameErrorKey()))
                    }
                }
            )
        }
    }

    private fun Throwable.toNameErrorKey(): String = when {
        this is InvalidUserNameException -> when (error) {
            UserNameError.EMPTY -> "error_name_empty"
            UserNameError.TOO_LONG -> "error_name_too_long"
        }
        this is AuthFailure && reason == AuthFailureReason.SESSION_EXPIRED -> "error_session_expired"
        else -> "error_save_name"
    }

    private fun performSignOut() {
        _uiState.update { it.copy(isSigningOut = true, error = false) }
        viewModelScope.launch {
            signOutUseCase().onFailure { _uiState.update { it.copy(isSigningOut = false, error = true) } }
        }
    }
}

/** El diálogo del nombre, mientras está abierto. */
data class NameDialogState(
    val draft: String = "",
    val isSaving: Boolean = false,
    val errorKey: String? = null
)

data class AccountUiState(
    val name: String = "",
    val email: String = "",
    val provider: SignInProvider = SignInProvider.PASSWORD,
    val nameDialog: NameDialogState? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isSigningOut: Boolean = false,
    val error: Boolean = false
)

sealed class AccountEvent {
    data class ThemeSelected(val mode: ThemeMode) : AccountEvent()
    data object SignOutClicked : AccountEvent()
    data object EditNameClicked : AccountEvent()
    data class NameDraftChanged(val name: String) : AccountEvent()
    data object NameSaveClicked : AccountEvent()
    data object NameDialogDismissed : AccountEvent()
}
