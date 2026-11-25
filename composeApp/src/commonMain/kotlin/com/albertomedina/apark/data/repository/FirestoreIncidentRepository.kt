package com.albertomedina.apark.data.repository

import com.albertomedina.apark.data.util.FirestoreConstants
import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.repository.IncidentRepository
import dev.gitlive.firebase.firestore.DoubleAsTimestampSerializer
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.ServerTimestampBehavior
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.fromMilliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlin.coroutines.cancellation.CancellationException

class FirestoreIncidentRepository(
    private val firestore: FirebaseFirestore
) : IncidentRepository {

    private fun incidents(vehicleId: String) = firestore
        .collection(FirestoreConstants.CARS_COLLECTION)
        .document(vehicleId)
        .collection(FirestoreConstants.INCIDENTS_COLLECTION)

    /**
     * La subcolección entera, sin filtros ni índice: la caducidad de las resueltas la mantiene
     * pequeña, y el móvil separa las abiertas de las resueltas (`incidentsView`).
     *
     * Las horas del servidor que aún no han llegado se leen **estimadas**: sin conexión, una
     * incidencia recién reportada dice «hace un momento» en vez de no tener fecha.
     *
     * Si quien mira deja de ser miembro, o el vehículo se borra, las reglas cortan la escucha con
     * PERMISSION_DENIED: se emite una lista vacía en vez de dejar que el error tumbe la pantalla, que
     * ya se encarga de decir que no hay acceso.
     */
    override fun observe(vehicleId: String): Flow<List<Incident>> = incidents(vehicleId)
        .snapshots
        .map { snapshot ->
            snapshot.documents.map { doc ->
                doc.data(IncidentDocument.serializer(), ServerTimestampBehavior.ESTIMATE).toDomain(doc.id)
            }
        }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }

    /**
     * Con un id que se genera aquí y `set`, y no con `add`, que espera al servidor para devolver el
     * documento y sin conexión no volvería nunca.
     */
    override suspend fun report(
        vehicleId: String,
        reportedBy: String,
        category: String,
        tag: String,
        note: String
    ): Result<Unit> = write {
        incidents(vehicleId).document.set(
            NewIncident.serializer(),
            NewIncident(category = category, tag = tag, note = note, reportedBy = reportedBy)
        ) { encodeDefaults = true }
    }

    override suspend fun resolve(
        vehicleId: String,
        incidentId: String,
        resolvedBy: String,
        expiresAtMillis: Long
    ): Result<Unit> = write {
        incidents(vehicleId).document(incidentId).update(
            STATUS to Incident.STATUS_RESOLVED,
            RESOLVED_BY to resolvedBy,
            RESOLVED_AT to FieldValue.serverTimestamp,
            EXPIRES_AT to Timestamp.fromMilliseconds(expiresAtMillis.toDouble())
        )
    }

    override suspend fun reopen(vehicleId: String, incidentId: String): Result<Unit> = write {
        incidents(vehicleId).document(incidentId).update(
            STATUS to Incident.STATUS_OPEN,
            RESOLVED_BY to FieldValue.delete,
            RESOLVED_AT to FieldValue.delete,
            EXPIRES_AT to FieldValue.delete
        )
    }

    override suspend fun delete(vehicleId: String, incidentId: String): Result<Unit> = write {
        incidents(vehicleId).document(incidentId).delete()
    }

    private suspend fun write(block: suspend () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private companion object {
        const val STATUS = "status"
        const val RESOLVED_BY = "resolvedBy"
        const val RESOLVED_AT = "resolvedAt"
        const val EXPIRES_AT = "expiresAt"
    }
}

/** Lo que se lee. Los campos de la resolución faltan mientras está abierta, de ahí los valores por defecto. */
@Serializable
private data class IncidentDocument(
    val category: String = "",
    val tag: String = "",
    val note: String = "",
    val status: String = Incident.STATUS_OPEN,
    val reportedBy: String = "",
    @Serializable(with = DoubleAsTimestampSerializer::class) val createdAt: Double = 0.0,
    val resolvedBy: String = "",
    @Serializable(with = DoubleAsTimestampSerializer::class) val resolvedAt: Double = 0.0,
    @Serializable(with = DoubleAsTimestampSerializer::class) val expiresAt: Double = 0.0
) {
    fun toDomain(id: String) = Incident(
        id = id,
        category = category,
        tag = tag,
        note = note,
        status = status,
        reportedBy = reportedBy,
        createdAt = createdAt.toLong(),
        resolvedBy = resolvedBy,
        resolvedAt = resolvedAt.toLong()
    )
}

/**
 * Lo que se escribe al reportar: exactamente los campos que aceptan las reglas, y la hora del
 * servidor, que es la que exigen (`createdAt == request.time`).
 */
@Serializable
private data class NewIncident(
    val category: String,
    val tag: String,
    val note: String,
    val status: String = Incident.STATUS_OPEN,
    val reportedBy: String,
    @Serializable(with = DoubleAsTimestampSerializer::class)
    val createdAt: Double = DoubleAsTimestampSerializer.SERVER_TIMESTAMP
)
