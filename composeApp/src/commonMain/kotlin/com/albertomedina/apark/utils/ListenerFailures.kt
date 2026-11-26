package com.albertomedina.apark.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Recoge el error de una escucha de Firestore (spec 021). Sin esto, la excepción se escapa de
 * `viewModelScope` y **cierra la app**, en Android y en iOS.
 *
 * Distingue dos casos:
 * - **Sin sesión, no es un fallo.** Al cerrarse la sesión, Firestore reenvía las escuchas sin
 *   credenciales y las reglas las rechazan, a menudo antes de que `authStateChanges` avise y
 *   `flatMapLatest` las cancele. No hay nada que registrar: la salida al login la lleva
 *   `SessionViewModel`.
 * - **Con sesión, es un fallo de verdad**, de las reglas o de la app: [onFailure].
 *
 * [isSignedIn] tiene que leer el usuario en el momento, con `getCurrentUser()`, y no con
 * `authStateChanges`. Firebase pone el usuario a `null` antes de avisar a Firestore, así que cuando
 * llega el rechazo ya se sabe que la sesión terminó; `authStateChanges` es justo la señal que llega
 * tarde.
 *
 * Va **dentro** del `flatMapLatest`, sobre la escucha de los datos. Fuera, el error terminaría
 * también la escucha de la sesión, y la pantalla dejaría de seguir a quien entre después.
 */
fun <T> Flow<T>.catchListenerFailure(
    isSignedIn: () -> Boolean,
    onFailure: (Throwable) -> Unit
): Flow<T> = catch { error -> if (isSignedIn()) onFailure(error) }
