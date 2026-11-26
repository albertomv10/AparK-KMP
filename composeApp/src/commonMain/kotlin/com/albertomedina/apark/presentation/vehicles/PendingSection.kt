package com.albertomedina.apark.presentation.vehicles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.model.Pending
import com.albertomedina.apark.domain.model.PendingIncident
import com.albertomedina.apark.presentation.components.SectionTitle
import com.albertomedina.apark.presentation.components.Who
import com.albertomedina.apark.presentation.components.whenWhoText
import com.albertomedina.apark.presentation.incidents.IncidentTypeIcon
import com.albertomedina.apark.presentation.incidents.TimeAgoSentences
import com.albertomedina.apark.presentation.incidents.displayName
import org.jetbrains.compose.resources.stringResource

/**
 * «Pendiente», arriba en *Vehículos* (spec 020): las incidencias abiertas de todos los vehículos, la
 * más reciente primero. Lo que justifica la pestaña: el carrusel dice **dónde está** cada vehículo, y
 * esto, **cómo está** (`docs/NAVIGATION.md`). El título de la lista, con «Ordenar», va aparte: se ve
 * también mientras se ordena.
 *
 * Mientras no se sabe nada no se pinta, para no decir «Todo en orden» sin saberlo. Si algún vehículo
 * falla, se dice, y lo de los demás se sigue viendo.
 *
 * Las claves llevan prefijo para no chocar con las de los vehículos, que son sus ids.
 */
fun LazyListScope.pendingSection(pending: Pending, onOpen: (vehicleId: String) -> Unit) {
    if (!pending.isShown) return

    item(key = "pending_title") {
        SectionTitle(stringResource(Res.string.pending_title), start = 16.dp)
    }
    items(pending.incidents, key = { "pending_${it.vehicleId}_${it.incident.id}" }) { row ->
        PendingIncidentRow(row, onClick = { onOpen(row.vehicleId) })
    }
    if (pending.failed) {
        item(key = "pending_status") {
            StatusLine(stringResource(Res.string.pending_load_error), MaterialTheme.colorScheme.error)
        }
    } else if (pending.incidents.isEmpty()) {
        item(key = "pending_status") {
            StatusLine(stringResource(Res.string.pending_all_clear), MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Hay algo que decir: incidencias, que no hay ninguna, o que no se han podido cargar. */
val Pending.isShown: Boolean
    get() = incidents.isNotEmpty() || complete || failed

/** Una incidencia: su icono, su nombre, y de qué vehículo es y cuándo se reportó. Sin quién. */
@Composable
private fun PendingIncidentRow(row: PendingIncident, onClick: () -> Unit) {
    val incident = row.incident
    val whenText = whenWhoText(incident.createdAt, Who.Unknown, TimeAgoSentences)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { IncidentTypeIcon(incident.kind) },
        headlineContent = {
            Text(
                incident.displayName(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                "${row.vehicleName} · $whenText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun StatusLine(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = color,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
