package com.albertomedina.apark.domain.auth

/**
 * Por qué no se completó un inicio de sesión con email y contraseña.
 *
 * Mismo principio que [com.albertomedina.apark.presentation.components.SocialLoginReason]: quien
 * conoce la excepción concreta la traduce a un motivo, y la UI decide qué enseñar sin tener que
 * interpretar cadenas. Antes se miraba si el mensaje de Firebase *contenía* la palabra
 * «credential», lo cual depende del idioma y de la versión del SDK, y cualquier otro fallo acababa
 * volcado tal cual en pantalla.
 */
enum class EmailLoginReason {
    /** Email o contraseña incorrectos, cuenta inexistente o deshabilitada. */
    INVALID_CREDENTIALS,

    /** Las credenciales valen, pero falta verificar el email. No es un error del usuario. */
    EMAIL_NOT_VERIFIED,

    /** Sin conexión: se puede reintentar sin cambiar nada. */
    NETWORK,

    /** Demasiados intentos seguidos; Firebase bloquea temporalmente. */
    TOO_MANY_ATTEMPTS,

    /** Cualquier otra cosa. Mensaje genérico, y el detalle a Crashlytics. */
    UNKNOWN
}

/**
 * [detail] es texto técnico. **Nunca se enseña al usuario**: existe para mandarlo a Crashlytics,
 * igual que en el login social.
 */
class EmailLoginFailure(
    val reason: EmailLoginReason,
    val detail: String? = null
) : Exception(detail ?: reason.name)
