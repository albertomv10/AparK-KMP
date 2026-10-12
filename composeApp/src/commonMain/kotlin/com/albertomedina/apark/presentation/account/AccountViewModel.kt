package com.albertomedina.apark.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.settings.SettingsRepository
import com.albertomedina.apark.domain.settings.ThemeMode
import com.albertomedina.apark.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val authRepository: AuthRepository,
    private val signOutUseCase: SignOutUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(email = authRepository.getUserEmail().orEmpty()) }
        viewModelScope.launch {
            settingsRepository.themeMode.collect { mode -> _uiState.update { it.copy(themeMode = mode) } }
        }
    }

    fun onEvent(event: AccountEvent) {
        when (event) {
            is AccountEvent.ThemeSelected -> viewModelScope.launch {
                settingsRepository.setThemeMode(event.mode)
            }

            is AccountEvent.SignOutClicked -> performSignOut()

            is AccountEvent.SignOutHandled -> _uiState.update { it.copy(signedOut = false) }
        }
    }

    private fun performSignOut() {
        _uiState.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            signOutUseCase().fold(
                onSuccess = { _uiState.update { it.copy(isSigningOut = false, signedOut = true) } },
                onFailure = { _uiState.update { it.copy(isSigningOut = false, error = true) } }
            )
        }
    }
}

data class AccountUiState(
    val email: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isSigningOut: Boolean = false,
    val signedOut: Boolean = false,
    val error: Boolean = false
)

sealed class AccountEvent {
    data class ThemeSelected(val mode: ThemeMode) : AccountEvent()
    data object SignOutClicked : AccountEvent()
    data object SignOutHandled : AccountEvent()
}
