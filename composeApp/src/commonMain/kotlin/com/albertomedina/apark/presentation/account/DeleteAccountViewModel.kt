package com.albertomedina.apark.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.auth.AuthFailure
import com.albertomedina.apark.domain.auth.AuthFailureReason
import com.albertomedina.apark.domain.auth.SignInProvider
import com.albertomedina.apark.domain.logging.CrashReporter
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.AccountDeletionImpact
import com.albertomedina.apark.domain.usecase.DeleteAccountUseCase
import com.albertomedina.apark.domain.usecase.DeletionProof
import com.albertomedina.apark.domain.usecase.GetAccountDeletionImpactUseCase
import com.albertomedina.apark.domain.usecase.SignOutUseCase
import com.albertomedina.apark.presentation.components.SocialLoginFailure
import com.albertomedina.apark.presentation.components.SocialLoginReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DeleteAccountViewModel(
    authRepository: AuthRepository,
    private val getAccountDeletionImpactUseCase: GetAccountDeletionImpactUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val crashReporter: CrashReporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeleteAccountUiState(provider = authRepository.signInProvider()))
    val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val impact = getAccountDeletionImpactUseCase()
            _uiState.update { it.copy(impact = impact) }
        }
    }

    fun onEvent(event: DeleteAccountEvent) {
        when (event) {
            is DeleteAccountEvent.PasswordChanged -> _uiState.update {
                it.copy(password = event.password, passwordError = null, generalError = null)
            }
            DeleteAccountEvent.SubmitPassword -> {
                if (_uiState.value.password.isEmpty()) {
                    _uiState.update { it.copy(passwordError = "error_empty_password") }
                } else {
                    delete(DeletionProof.Password(_uiState.value.password))
                }
            }
            is DeleteAccountEvent.GoogleTokenReceived -> delete(DeletionProof.Google(event.idToken, event.accessToken))
            is DeleteAccountEvent.GoogleFailed -> onGoogleFailed(event.failure)
            // Cerrar sesión hace que la sesión de las pestañas lleve al login, como en cualquier
            // otro cierre. La cuenta ya no existe en el servidor.
            DeleteAccountEvent.DoneAcknowledged -> viewModelScope.launch { signOutUseCase() }
        }
    }

    private fun delete(proof: DeletionProof) {
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, generalError = null) }
        viewModelScope.launch {
            deleteAccountUseCase(proof).fold(
                onSuccess = { _uiState.update { it.copy(isSubmitting = false, done = true, password = "") } },
                onFailure = { error -> onFailed(error) }
            )
        }
    }

    /** Lo de un campo va en ese campo; el resto, en el formulario, y lo desconocido a Crashlytics. */
    private fun onFailed(error: Throwable) {
        val reason = (error as? AuthFailure)?.reason
        _uiState.update {
            when (reason) {
                AuthFailureReason.INVALID_CREDENTIALS ->
                    it.copy(isSubmitting = false, passwordError = "error_wrong_current_password")
                // Pasaron más de cinco minutos entre reautenticar y llamar al servidor. Nada se ha
                // borrado, y basta con volver a confirmar.
                AuthFailureReason.REQUIRES_RECENT_LOGIN ->
                    it.copy(isSubmitting = false, generalError = "error_reauth_again")
                else -> it.copy(
                    isSubmitting = false,
                    generalError = error.generalAccountErrorKey(crashReporter, DELETE_ACCOUNT_CONTEXT)
                )
            }
        }
    }

    /** Cancelar el selector de Google es una decisión, no un fallo: no se avisa de nada. */
    private fun onGoogleFailed(failure: SocialLoginFailure) {
        if (failure.reason == SocialLoginReason.CANCELLED) return
        crashReporter.recordFailure(DELETE_ACCOUNT_CONTEXT, failure.detail)
        val key = if (failure.reason == SocialLoginReason.NO_ACCOUNTS) "error_social_no_accounts" else "error_google_login"
        _uiState.update { it.copy(generalError = key) }
    }

    companion object {
        const val DELETE_ACCOUNT_CONTEXT = "delete_account_failed"
    }
}

data class DeleteAccountUiState(
    val provider: SignInProvider = SignInProvider.PASSWORD,
    /** `null` mientras se calcula. */
    val impact: AccountDeletionImpact? = null,
    val password: String = "",
    val passwordError: String? = null,
    val generalError: String? = null,
    val isSubmitting: Boolean = false,
    val done: Boolean = false
)

sealed class DeleteAccountEvent {
    data class PasswordChanged(val password: String) : DeleteAccountEvent()
    data object SubmitPassword : DeleteAccountEvent()
    data class GoogleTokenReceived(val idToken: String, val accessToken: String?) : DeleteAccountEvent()
    data class GoogleFailed(val failure: SocialLoginFailure) : DeleteAccountEvent()
    data object DoneAcknowledged : DeleteAccountEvent()
}
