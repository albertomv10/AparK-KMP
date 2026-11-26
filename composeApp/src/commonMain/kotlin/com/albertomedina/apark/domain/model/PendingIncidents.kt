package com.albertomedina.apark.domain.model

/**
 * Lo que se sabe de las incidencias abiertas de un vehículo (spec 020): lo que tiene, o que no se
 * han podido cargar. Mientras no hay respuesta, el vehículo simplemente no está en el mapa.
 */
sealed interface OpenIncidents {
    /** Las abiertas, la más reciente primero. */
    data class Loaded(val incidents: List<Incident>) : OpenIncidents

    data object Failed : OpenIncidents
}

/** Una fila de «Pendiente»: la incidencia y de qué vehículo es. */
data class PendingIncident(val vehicleId: String, val vehicleName: String, val incident: Incident)

/**
 * Lo pendiente de todos los vehículos, para la sección de *Vehículos*.
 *
 * @property complete todos los vehículos tienen ya respuesta. Hasta entonces no se puede decir «Todo
 *   en orden»: no se sabe.
 * @property failed alguno no se ha podido cargar, así que lo que hay puede no ser todo.
 */
data class Pending(val incidents: List<PendingIncident>, val complete: Boolean, val failed: Boolean)

/**
 * Lo pendiente de [vehicles], con lo que se sabe de cada uno en [open]. Solo cuentan los vehículos de
 * la lista: lo de uno que ya no está —porque te sacaron, o porque se borró— se ignora aunque su
 * escucha aún no se haya cerrado (spec 020, criterio 13).
 */
fun pendingOf(vehicles: List<Vehicle>, open: Map<String, OpenIncidents>): Pending {
    val incidents = vehicles
        .flatMap { vehicle ->
            val loaded = open[vehicle.id] as? OpenIncidents.Loaded ?: return@flatMap emptyList()
            loaded.incidents.map { PendingIncident(vehicle.id, vehicle.name, it) }
        }
        .sortedByDescending { it.incident.createdAt }
    return Pending(
        incidents = incidents,
        complete = vehicles.all { it.id in open },
        failed = vehicles.any { open[it.id] == OpenIncidents.Failed }
    )
}

/** Cuántas incidencias abiertas tienen los vehículos de [vehicleIds]: la chapita de la pestaña. */
fun openCount(vehicleIds: Set<String>, open: Map<String, OpenIncidents>): Int =
    vehicleIds.sumOf { id -> (open[id] as? OpenIncidents.Loaded)?.incidents?.size ?: 0 }
