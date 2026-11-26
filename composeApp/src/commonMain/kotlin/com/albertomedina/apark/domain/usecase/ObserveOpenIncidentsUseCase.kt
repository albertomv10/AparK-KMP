package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.model.OpenIncidents
import com.albertomedina.apark.domain.model.openNewestFirst
import com.albertomedina.apark.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Las incidencias abiertas de varios vehículos a la vez, por su id (spec 020): para «Pendiente», la
 * tarjeta del mapa y la chapita.
 *
 * **Una escucha por vehículo**, la misma consulta que abre el detalle. El SDK de Firestore comparte
 * las escuchas de una misma consulta, así que abrirla desde varios sitios no multiplica las
 * lecturas. Con diez vehículos como mucho no hace falta una consulta de grupo de colección
 * (`DATA-MODEL.md` §4).
 *
 * **Cada escucha recoge su propio error**, como `catchListenerFailure` (spec 021): con la sesión
 * viva llama a [onFailure] y ese vehículo pasa a [OpenIncidents.Failed]; sin sesión no hace nada.
 * Dentro del `combine`, una que falla no corta las demás.
 *
 * Emite cuando todos han respondido una vez. Quien lo llama lo hace dentro de un `flatMapLatest`
 * sobre los ids, así que las escuchas se reabren solo cuando cambia el conjunto de vehículos.
 */
class ObserveOpenIncidentsUseCase(private val repository: IncidentRepository) {
    operator fun invoke(
        vehicleIds: Set<String>,
        isSignedIn: () -> Boolean,
        onFailure: (Throwable) -> Unit
    ): Flow<Map<String, OpenIncidents>> {
        if (vehicleIds.isEmpty()) return flowOf(emptyMap())
        val perVehicle = vehicleIds.map { id ->
            repository.observe(id)
                .map<_, OpenIncidents> { OpenIncidents.Loaded(it.openNewestFirst()) }
                .catch { error ->
                    if (isSignedIn()) {
                        onFailure(error)
                        emit(OpenIncidents.Failed)
                    }
                }
                .map { id to it }
        }
        return combine(perVehicle) { it.toMap() }.distinctUntilChanged()
    }
}
