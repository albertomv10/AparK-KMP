package com.albertomedina.apark.presentation.vehicles

import com.albertomedina.apark.domain.model.Vehicle

/**
 * Los vehículos en el orden de [ids] (spec 016). Los que no están en [ids] van al final, en el orden
 * en que llegan, y los ids que ya no tienen vehículo se ignoran.
 *
 * Es lo mismo que hace el repositorio con `userVehicles`, y por la misma razón: si mientras se ordena
 * entra un vehículo nuevo o desaparece uno, se ve sin perder lo que ya se ha movido.
 */
fun List<Vehicle>.inOrder(ids: List<String>): List<Vehicle> {
    val position = ids.withIndex().associate { (index, id) -> id to index }
    // `sortedBy` es estable: los que no están en `ids` conservan entre sí el orden en que llegaron.
    return sortedBy { position[it.id] ?: Int.MAX_VALUE }
}

/** La lista con el elemento de [from] movido a [to]. Fuera de rango, la lista tal cual. */
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
