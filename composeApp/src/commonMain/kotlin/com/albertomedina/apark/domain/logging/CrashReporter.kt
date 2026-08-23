package com.albertomedina.apark.domain.logging

/**
 * Dónde van a parar los fallos que el usuario no debe leer.
 *
 * Existe porque no tenerlo salió caro: el `onError` de los botones sociales hacía un `println`, y
 * en un móvil con MIUI —que suprime los logs de apps de terceros— eso significaba que un error de
 * configuración de OAuth no dejaba rastro **en ningún sitio**. Enseñar el detalle técnico al
 * usuario no es la solución; tener dónde mandarlo, sí.
 *
 * Se inyecta como interfaz para que la capa de presentación no dependa de Firebase, y para poder
 * sustituirlo por un doble en los tests.
 */
interface CrashReporter {

    /** Miga de pan: no genera un informe, pero acompaña al siguiente que se envíe. */
    fun log(message: String)

    /** Un fallo del que la app se ha recuperado. Llega a Crashlytics como *non-fatal*. */
    fun recordError(error: Throwable, context: String? = null)

    /**
     * Un fallo del que solo tenemos texto, típicamente venido de un SDK de plataforma.
     * [context] agrupa los informes, así que debe ser estable; el detalle variable va en [detail].
     */
    fun recordFailure(context: String, detail: String?)

    /**
     * Asocia los informes a un usuario, para poder responder a un "a mí no me funciona".
     * Se pasa **el uid, nunca el email**: identifica igual y no es un dato personal en claro.
     * Con `null` se desvincula, que es lo que toca al cerrar sesión.
     */
    fun setUserId(userId: String?)
}
