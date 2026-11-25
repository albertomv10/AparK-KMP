package com.albertomedina.apark.domain.repository

import com.albertomedina.apark.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(userId: String): Flow<User>
    suspend fun createUser(user: User)

    /**
     * Guarda el orden de los vehículos de esta persona: el del carrusel y el de la lista (spec 016).
     *
     * `userVehicles` es solo una pista de orden (spec 008): un vehículo que no esté en ella sale al
     * final. Por eso basta una escritura normal y no una transacción, que fallaría sin conexión. Si
     * mientras tanto entra un vehículo nuevo, lo peor que pasa es que se queda el último.
     *
     * **No termina hasta que el servidor confirma**, y sin conexión no termina nunca, aunque el caché
     * local ya enseñe el orden nuevo. Quien la llama no debe esperarla para seguir.
     */
    suspend fun setVehicleOrder(userId: String, vehicleIds: List<String>): Result<Unit>

    /**
     * The name other members see on the cards of the vehicles this person parked. Only this field
     * is written: the copies inside the vehicles are the `propagateUserProfile` function's job.
     */
    suspend fun updateName(userId: String, name: String): Result<Unit>

    /**
     * Brings the stored email in line with the account's after it changed in Auth. Does nothing if
     * it already matches, or if the document does not exist yet.
     */
    suspend fun syncEmail(userId: String, email: String): Result<Unit>
}