package com.albertomedina.apark.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.repository.AuthRepository
import com.albertomedina.apark.domain.usecase.SyncUserProfileUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * La sesión mientras se está dentro de las pestañas. Vive en `TabShell`, así que nace al entrar y
 * muere al salir.
 *
 * Es **la única salida** de la app: cuando la sesión termina, venga de donde venga, esto lleva al
 * login. Antes solo salía el botón de cerrar sesión, y la sesión también puede morir **fuera** de
 * la app. Firebase la revoca al cambiar el email o la contraseña, y si nadie escuchaba, la app se
 * quedaba en unas pestañas en las que todo devolvía error (spec 011).
 */
class SessionViewModel(
    authRepository: AuthRepository,
    private val syncUserProfileUseCase: SyncUserProfileUseCase
) : ViewModel() {

    private val uid = authRepository.authStateChanges
        .map { it?.uid }
        // También emite al refrescarse el token, cada hora más o menos; eso no es un cambio.
        .distinctUntilChanged()

    /** `true` cuando ya no hay nadie dentro. */
    val signedOut: StateFlow<Boolean> = uid
        .map { it == null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)

    init {
        viewModelScope.launch {
            uid.filterNotNull().collect {
                // Si falla —sin red, típicamente— no pasa nada: se vuelve a intentar en la próxima
                // entrada, y mientras tanto lo único que se retrasa es el email de las tarjetas.
                syncUserProfileUseCase()
            }
        }
    }
}
