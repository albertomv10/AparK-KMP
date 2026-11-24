package com.albertomedina.apark.domain.repository

/** Lo que solo el servidor puede hacer con la cuenta entera. */
interface AccountRepository {
    /**
     * Borra la cuenta de quien llama y todo lo que dejó (spec 012). Exige haber reautenticado hace
     * poco; si no, falla con
     * [com.albertomedina.apark.domain.auth.AuthFailureReason.REQUIRES_RECENT_LOGIN] sin tocar nada.
     */
    suspend fun deleteAccount(): Result<Unit>
}
