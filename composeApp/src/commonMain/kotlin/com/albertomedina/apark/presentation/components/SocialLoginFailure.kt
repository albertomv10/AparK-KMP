package com.albertomedina.apark.presentation.components

/**
 * Por qué no se completó un login social.
 *
 * La capa de plataforma conoce la excepción concreta; la UI no debería tener que interpretar
 * cadenas para decidir qué enseñar. Por eso el botón devuelve un motivo y no un texto.
 */
enum class SocialLoginReason {
    /** El usuario cerró el diálogo a propósito. **No es un error y no se muestra nada.** */
    CANCELLED,

    /** No hay ninguna cuenta del proveedor en el dispositivo: el usuario puede arreglarlo. */
    NO_ACCOUNTS,

    /** Cualquier otra cosa. El usuario ve un mensaje genérico; el detalle es para diagnóstico. */
    UNKNOWN
}

/**
 * [detail] es texto técnico (tipo de excepción y mensaje). **Nunca se enseña al usuario**: existe
 * para poder mandarlo a Crashlytics cuando exista (fase 0.5 del roadmap). Mostrarlo fue un parche
 * para diagnosticar un fallo silencioso, no un comportamiento aceptable.
 */
data class SocialLoginFailure(
    val reason: SocialLoginReason,
    val detail: String? = null
)
