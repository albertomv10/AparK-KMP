package com.albertomedina.apark.data.repository

/** En iOS el código va en el mensaje del `NSError`; ver `firebaseAuthErrorCode()`. */
internal actual fun Throwable.nativeAuthErrorCode(): String? = null
