package com.albertomedina.apark.presentation.components

/**
 * La rellena Swift al arrancar la app.
 *
 * `onError` recibe `(mensaje, cancelledByUser)`. El booleano existe porque desde Kotlin no hay
 * forma de distinguir una cancelación de un fallo real: sólo llegaría `localizedDescription`, y
 * decidir mirando ese texto sería frágil y dependiente del idioma del dispositivo.
 */
var iosGoogleSignInProvider: ((
    onSuccess: (String, String) -> Unit,
    onError: (String, Boolean) -> Unit
) -> Unit)? = null
