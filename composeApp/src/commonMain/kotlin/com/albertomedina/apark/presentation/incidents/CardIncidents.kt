package com.albertomedina.apark.presentation.incidents

import com.albertomedina.apark.domain.model.Incident

/**
 * Lo que enseña la tarjeta del mapa de las incidencias abiertas de un vehículo (spec 020): un icono
 * por tipo y cuántos tipos más no caben.
 *
 * @property shown la incidencia más reciente de cada tipo, la más reciente primero. Da el icono, y
 *   el nombre para el lector de pantalla: una personalizada se anuncia por el suyo.
 */
data class CardIncidents(val shown: List<Incident>, val more: Int)

/**
 * Dos incidencias del mismo tipo son un solo icono: lo que importa en la tarjeta es qué le pasa, no
 * cuántas veces se ha dicho. Como mucho [max], los de las más recientes, y el resto se cuenta.
 */
fun cardIncidents(open: List<Incident>, max: Int = 3): CardIncidents {
    val oneByType = open.sortedByDescending { it.createdAt }.distinctBy { it.kind }
    return CardIncidents(shown = oneByType.take(max), more = (oneByType.size - max).coerceAtLeast(0))
}
