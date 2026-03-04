package com.albertomedina.apark.presentation.components

// Esta variable guardará la función de Swift.
// Inicialmente es nula, Swift la rellenará al arrancar la app.
var iosGoogleSignInProvider: ((onSuccess: (String, String) -> Unit, onError: (String) -> Unit) -> Unit)? = null