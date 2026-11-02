package com.albertomedina.apark.presentation.account

import com.albertomedina.apark.domain.auth.AuthFailure
import com.albertomedina.apark.domain.auth.AuthFailureReason
import com.albertomedina.apark.domain.logging.CrashReporter

/**
 * La llave del mensaje para un fallo que **no es de ningún campo**, en los formularios de la cuenta.
 *
 * Cada formulario resuelve antes los que sí lo son —contraseña actual incorrecta, email en uso—,
 * porque esos se enseñan debajo de su campo. Lo que no se sabe clasificar va a Crashlytics, con el
 * detalle técnico que nunca se enseña.
 */
internal fun Throwable.generalAccountErrorKey(crashReporter: CrashReporter, context: String): String {
    val failure = this as? AuthFailure ?: AuthFailure(AuthFailureReason.UNKNOWN, detail = message)
    return when (failure.reason) {
        AuthFailureReason.NETWORK -> "error_no_network"
        AuthFailureReason.TOO_MANY_ATTEMPTS -> "error_too_many_attempts"
        // El repositorio ya ha cerrado la sesión, y la app va camino del login.
        AuthFailureReason.SESSION_EXPIRED -> "error_session_expired"
        // Ninguno debería llegar aquí: o los resuelve el formulario en su campo, o no pueden salir
        // de un cambio de cuenta. Si llegan, se quiere saber.
        AuthFailureReason.INVALID_CREDENTIALS,
        AuthFailureReason.EMAIL_NOT_VERIFIED,
        AuthFailureReason.EMAIL_ALREADY_IN_USE,
        AuthFailureReason.WEAK_PASSWORD,
        AuthFailureReason.INVALID_EMAIL,
        AuthFailureReason.REQUIRES_RECENT_LOGIN,
        AuthFailureReason.UNKNOWN -> {
            crashReporter.recordFailure(context, failure.detail ?: failure.reason.name)
            "error_unknown"
        }
    }
}
