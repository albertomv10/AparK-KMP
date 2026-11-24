package com.albertomedina.apark.data.repository

import com.albertomedina.apark.domain.auth.AuthFailure
import com.albertomedina.apark.domain.auth.AuthFailureReason
import com.albertomedina.apark.domain.repository.AccountRepository
import dev.gitlive.firebase.functions.FirebaseFunctions
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException

class FirebaseAccountRepository(
    private val functions: FirebaseFunctions
) : AccountRepository {

    /**
     * El login caducado vuelve como estado y no como error, igual que los desenlaces esperados de
     * `joinVehicleWithCode`: gitlive no deja leer el código de un `HttpsError` desde aquí. Lo demás
     * es inesperado, y llega como [AuthFailureReason.UNKNOWN] con su detalle para Crashlytics.
     */
    override suspend fun deleteAccount(): Result<Unit> = try {
        val response = functions.httpsCallable("deleteAccount").invoke(DeleteAccountRequest()).data<DeleteAccountResponse>()
        when (response.status) {
            "deleted" -> Result.success(Unit)
            "requires-recent-login" -> Result.failure(AuthFailure(AuthFailureReason.REQUIRES_RECENT_LOGIN))
            else -> Result.failure(AuthFailure(AuthFailureReason.UNKNOWN, detail = "status=${response.status}"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AuthFailure(AuthFailureReason.UNKNOWN, detail = "${e::class.simpleName}: ${e.message}"))
    }

    /** Sin datos: la callable sabe quién llama por el token. Llega como `{}`. */
    @Serializable
    private class DeleteAccountRequest

    @Serializable
    private data class DeleteAccountResponse(val status: String)
}
