package com.albertomedina.apark.domain.auth

/**
 * Por qué no salió una operación de Firebase Auth: entrar, registrarse, reautenticarse, o cambiar
 * el email o la contraseña.
 *
 * Mismo principio que [com.albertomedina.apark.presentation.components.SocialLoginReason]: quien
 * conoce la excepción concreta la traduce a un motivo, y la UI decide qué enseñar sin tener que
 * interpretar cadenas. Antes se miraba si el mensaje de Firebase *contenía* la palabra
 * «credential», lo cual depende del idioma y de la versión del SDK, y cualquier otro fallo acababa
 * volcado tal cual en pantalla.
 *
 * Empezó llamándose `EmailLoginFailure`, cuando solo servía al login. La spec 011 lo extendió al
 * resto de operaciones, y un mismo motivo puede querer decir cosas distintas según cuál sea:
 * [AuthFailureReason.INVALID_CREDENTIALS] es «email o contraseña mal» al entrar, y «tu contraseña
 * actual no es esa» al reautenticarse. Por eso la traducción a texto la hace cada ViewModel.
 */
enum class AuthFailureReason {
    /** Email o contraseña incorrectos, cuenta inexistente o deshabilitada. */
    INVALID_CREDENTIALS,

    /** Las credenciales valen, pero falta verificar el email. No es un error del usuario. */
    EMAIL_NOT_VERIFIED,

    /** Al registrarse o al cambiar el email: esa dirección ya tiene una cuenta. */
    EMAIL_ALREADY_IN_USE,

    /** Firebase rechaza la contraseña nueva. La validación local es más estricta, así que es raro. */
    WEAK_PASSWORD,

    /** Firebase no acepta el formato del email. La validación local también lo filtra antes. */
    INVALID_EMAIL,

    /**
     * La operación exige haber entrado hace poco. No debería verse nunca: los cambios de email y
     * contraseña reautentican justo antes, precisamente para que esto no ocurra.
     */
    REQUIRES_RECENT_LOGIN,

    /**
     * La sesión ya no vale: se cambió el email o la contraseña, o la cuenta se deshabilitó. Firebase
     * revoca la sesión ante esos cambios, y la app tiene que volver al login.
     */
    SESSION_EXPIRED,

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
class AuthFailure(
    val reason: AuthFailureReason,
    val detail: String? = null
) : Exception(detail ?: reason.name)
