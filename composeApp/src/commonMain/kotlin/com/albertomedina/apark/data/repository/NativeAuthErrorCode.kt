package com.albertomedina.apark.data.repository

/**
 * El código de error del SDK nativo de Firebase Auth, como `ERROR_USER_MISMATCH`.
 *
 * Solo existe en Android, y desde el código común no se ve: gitlive reexporta las excepciones de
 * Android como `typealias` de las nativas, pero su `errorCode` no forma parte del API común. En
 * iOS el código es un número dentro del mensaje, y lo lee `firebaseAuthErrorCode()`.
 */
internal expect fun Throwable.nativeAuthErrorCode(): String?
