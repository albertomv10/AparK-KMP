package com.albertomedina.apark.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Una incidencia de un vehículo (spec 019): algo que le pasa, reportado por un miembro y resuelto
 * por cualquiera. El tipo va en texto, como en el documento, y se traduce con [kind] solo para
 * mostrarlo. [title] es el nombre de una personalizada; en las demás, `""`.
 *
 * De quién la reportó o la resolvió se guarda **solo el uid**: el nombre sale de la lista de miembros
 * del detalle, y si ya no está —se fue, o borró su cuenta— no se enseña ninguno.
 */
data class Incident(
    val id: String,
    val type: String,
    val title: String = "",
    val note: String = "",
    val status: String = STATUS_OPEN,
    val reportedBy: String = "",
    /** En milisegundos. La pone el servidor; sin conexión, una estimación. */
    val createdAt: Long = 0L,
    val resolvedBy: String = "",
    val resolvedAt: Long = 0L
) {
    val isResolved: Boolean get() = status == STATUS_RESOLVED

    /** Su tipo. Uno desconocido es [IncidentType.CUSTOM], y sin [title] se enseña «Incidencia». */
    val kind: IncidentType get() = IncidentType.fromCode(type)

    companion object {
        const val STATUS_OPEN = "open"
        const val STATUS_RESOLVED = "resolved"
        const val NOTE_MAX_LENGTH = 280
        /** El nombre de una personalizada: un título que cabe en una línea; lo largo va en la nota. */
        const val TITLE_MAX_LENGTH = 40

        /**
         * Lo que una incidencia resuelta se queda a la vista. Después Firestore la borra sola: la
         * caducidad (TTL) de `incidents` sobre `expiresAt`, que se crea a mano en cada base.
         */
        val RESOLVED_RETENTION: Duration = 30.days
    }
}

data class IncidentsView(val open: List<Incident>, val resolved: List<Incident>)

/**
 * Las incidencias de un vehículo, como se enseñan: las abiertas, de la más reciente a la más
 * antigua; y las resueltas de los últimos 30 días, por fecha de resolución. Las más viejas se
 * filtran aquí aunque Firestore aún no las haya borrado: la caducidad puede tardar hasta un día.
 */
fun incidentsView(incidents: List<Incident>, nowMillis: Long): IncidentsView {
    val retention = Incident.RESOLVED_RETENTION.inWholeMilliseconds
    return IncidentsView(
        open = incidents.openNewestFirst(),
        resolved = incidents
            .filter { it.isResolved && nowMillis - it.resolvedAt < retention }
            .sortedByDescending { it.resolvedAt }
    )
}

/** Las abiertas, de la más reciente a la más antigua: el orden del detalle y de «Pendiente». */
fun List<Incident>.openNewestFirst(): List<Incident> =
    filterNot { it.isResolved }.sortedByDescending { it.createdAt }
