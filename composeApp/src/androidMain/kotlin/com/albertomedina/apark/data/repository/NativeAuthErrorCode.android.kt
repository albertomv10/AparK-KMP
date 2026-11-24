package com.albertomedina.apark.data.repository

import com.google.firebase.auth.FirebaseAuthException

internal actual fun Throwable.nativeAuthErrorCode(): String? = (this as? FirebaseAuthException)?.errorCode
