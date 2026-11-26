package com.albertomedina.apark.presentation.incidents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.model.Fuel
import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.model.IncidentCategory
import com.albertomedina.apark.domain.model.IncidentType
import com.albertomedina.apark.domain.model.Powertrain
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.incidentsView
import com.albertomedina.apark.presentation.components.AparKConfirmDialog
import com.albertomedina.apark.presentation.components.StandardAparKButton
import com.albertomedina.apark.presentation.components.whenWhoText
import com.albertomedina.apark.presentation.components.whoIs
import com.albertomedina.apark.utils.SnackbarMessage
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * La sección «Incidencias» del detalle (spec 019): las abiertas, el botón de reportar y las resueltas
 * de los últimos 30 días, plegadas.
 *
 * @param names el nombre de cada miembro por su uid, de la lista que ya carga el detalle. Quien ya no
 *   es miembro no está, y su incidencia sale solo con cuándo (criterio 2).
 * @param showSnackbar el aviso lo pinta el `Scaffold` del detalle, que es el que tiene el hueco.
 */
@Composable
fun ColumnScope.IncidentsSection(
    vehicle: Vehicle,
    isOwner: Boolean,
    names: Map<String, String>,
    title: @Composable () -> Unit,
    showSnackbar: suspend (SnackbarMessage) -> SnackbarResult,
    viewModel: IncidentsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val onEvent = viewModel::onEvent

    LaunchedEffect(vehicle.id) { onEvent(IncidentsEvent.Started(vehicle.id)) }

    val resolvedText = stringResource(Res.string.incidents_resolved_snackbar)
    val undoText = stringResource(Res.string.undo)
    val errorText = stringResource(Res.string.incidents_error)
    LaunchedEffect(state.message) {
        when (val message = state.message ?: return@LaunchedEffect) {
            is IncidentsMessage.Resolved -> {
                val result = showSnackbar(SnackbarMessage.Success(resolvedText, actionLabel = undoText))
                if (result == SnackbarResult.ActionPerformed) onEvent(IncidentsEvent.Undo(message.incidentId))
            }
            IncidentsMessage.Error -> showSnackbar(SnackbarMessage.Error(errorText))
        }
        onEvent(IncidentsEvent.MessageShown)
    }

    // Las resueltas de más de 30 días se ocultan aunque la caducidad de Firestore aún no las haya
    // borrado (criterio 10). La hora se toma en cada recomposición: basta, no hace falta un reloj.
    val view = incidentsView(state.incidents, Clock.System.now().toEpochMilliseconds())
    val currentUserId = state.currentUserId

    title()
    if (state.loadFailed) {
        // Sin esto, una escucha que falla se leería como «Sin incidencias abiertas».
        Text(
            stringResource(Res.string.incidents_load_error),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    } else if (view.open.isEmpty()) {
        Text(
            stringResource(Res.string.incidents_none_open),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
    }
    view.open.forEach { incident ->
        IncidentItem(
            incident = incident,
            byline = whenWhoText(incident.createdAt, whoIs(incident.reportedBy, currentUserId, names), ReportedSentences),
            canDelete = currentUserId != null && (incident.reportedBy == currentUserId || isOwner),
            onResolve = { onEvent(IncidentsEvent.Resolve(incident)) },
            onDelete = { onEvent(IncidentsEvent.DeleteRequested(incident)) }
        )
    }

    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        OutlinedButton(onClick = { onEvent(IncidentsEvent.ReportOpened) }, enabled = currentUserId != null) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.incidents_report))
        }
    }

    if (view.resolved.isNotEmpty()) {
        ResolvedHeader(count = view.resolved.size, expanded = state.showResolved) { onEvent(IncidentsEvent.ResolvedToggled) }
        if (state.showResolved) {
            view.resolved.forEach { incident ->
                IncidentItem(
                    incident = incident,
                    byline = whenWhoText(incident.resolvedAt, whoIs(incident.resolvedBy, currentUserId, names), ResolvedSentences),
                    canDelete = currentUserId != null && (incident.reportedBy == currentUserId || isOwner),
                    onResolve = null,
                    onDelete = { onEvent(IncidentsEvent.DeleteRequested(incident)) }
                )
            }
        }
    }

    state.form?.let { form ->
        ReportSheet(
            form = form,
            powertrain = Powertrain.fromCode(vehicle.powertrain),
            fuel = Fuel.fromCode(vehicle.fuel),
            onEvent = onEvent
        )
    }

    if (state.pendingDelete != null) {
        AparKConfirmDialog(
            title = stringResource(Res.string.incidents_delete_title),
            text = stringResource(Res.string.incidents_delete_message),
            confirmLabel = stringResource(Res.string.incidents_delete),
            dismissLabel = stringResource(Res.string.cancel),
            onConfirm = { onEvent(IncidentsEvent.DeleteConfirmed) },
            onDismiss = { onEvent(IncidentsEvent.DeleteDismissed) },
            isDestructive = true
        )
    }
}

/**
 * Una incidencia: su icono, su nombre, la nota y quién y cuándo. Los tres textos tienen que
 * distinguirse a simple vista (criterio 1): el nombre con peso, la nota normal y atenuada, y quién y
 * cuándo más pequeño y separado de la nota, que antes iba pegado a ella.
 *
 * @param onResolve `null` en las resueltas, que no tienen botón.
 */
@Composable
private fun IncidentItem(
    incident: Incident,
    byline: String,
    canDelete: Boolean,
    onResolve: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val resolved = incident.isResolved
    val dimmed = MaterialTheme.colorScheme.onSurfaceVariant
    var menuOpen by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                incident.displayName(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = if (resolved) dimmed else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Column {
                if (incident.note.isNotBlank()) {
                    Text(
                        incident.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = dimmed,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text(
                    byline,
                    style = MaterialTheme.typography.bodySmall,
                    color = dimmed,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        },
        leadingContent = {
            // Las resueltas, en gris, que ya no piden atención.
            IncidentTypeIcon(incident.kind, color = if (resolved) dimmed else incident.kind.color)
        },
        trailingContent = if (onResolve == null && !canDelete) null else {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onResolve != null) {
                        IconButton(onClick = onResolve) {
                            Icon(
                                Icons.Default.CheckCircleOutline,
                                contentDescription = stringResource(Res.string.incidents_resolve),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (canDelete) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(Res.string.incidents_more_actions))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.incidents_delete), color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuOpen = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

/** «Resueltas (n)», que pliega y despliega la lista. */
@Composable
private fun ResolvedHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = expanded, onClick = onToggle, role = Role.Button)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(Res.string.incidents_resolved_title, count),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * El formulario de reportar (spec 019). La categoría solo agrupa: lo que se elige es qué le pasa, de
 * entre los tipos que tienen sentido para este vehículo (criterio 5). Lo que no encaje es
 * «Personalizada», con su nombre. La nota es opcional en los dos casos.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ReportSheet(
    form: ReportForm,
    powertrain: Powertrain?,
    fuel: Fuel?,
    onEvent: (IncidentsEvent) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = { onEvent(IncidentsEvent.ReportDismissed) }, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(Res.string.incidents_report), style = MaterialTheme.typography.titleLarge)

            Text(stringResource(Res.string.incidents_form_type), style = MaterialTheme.typography.titleSmall)

            // Todos los tipos a la vez, separados por categorías: se toca directamente el que es.
            IncidentCategory.entries.forEach { category ->
                val types = category.types.filter { it.appliesTo(powertrain, fuel) }
                if (types.isNotEmpty()) {
                    TypeGroup(
                        heading = stringResource(category.label),
                        headingIcon = category.icon,
                        color = category.color,
                        types = types,
                        selected = form.type,
                        onSelect = { onEvent(IncidentsEvent.TypeSelected(it)) }
                    )
                }
            }
            TypeGroup(
                heading = stringResource(Res.string.incidents_form_not_listed),
                headingIcon = null,
                color = IncidentType.CUSTOM.color,
                types = listOf(IncidentType.CUSTOM),
                selected = form.type,
                onSelect = { onEvent(IncidentsEvent.TypeSelected(it)) }
            )
            if (form.isCustom) {
                OutlinedTextField(
                    value = form.title,
                    onValueChange = { onEvent(IncidentsEvent.TitleChanged(it)) },
                    label = { Text(stringResource(Res.string.incidents_form_title)) },
                    supportingText = { Counter(form.title.length, Incident.TITLE_MAX_LENGTH) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = form.note,
                onValueChange = { onEvent(IncidentsEvent.NoteChanged(it)) },
                label = { Text(stringResource(Res.string.incidents_form_note)) },
                supportingText = { Counter(form.note.length, Incident.NOTE_MAX_LENGTH) },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StandardAparKButton(
                    onClick = { onEvent(IncidentsEvent.ReportConfirmed) },
                    enabled = form.canSubmit
                ) {
                    Text(stringResource(Res.string.incidents_form_submit))
                }
            }
        }
    }
}

/**
 * Un grupo de tipos con su encabezado. Los chips llevan el color de su categoría en el icono y, si
 * están elegidos, en el fondo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeGroup(
    heading: String,
    headingIcon: ImageVector?,
    color: Color,
    types: List<IncidentType>,
    selected: IncidentType?,
    onSelect: (IncidentType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (headingIcon != null) {
                Icon(headingIcon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(heading, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            types.forEach { type ->
                FilterChip(
                    selected = selected == type,
                    onClick = { onSelect(type) },
                    label = { Text(stringResource(type.label)) },
                    leadingIcon = { Icon(type.icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) },
                    colors = FilterChipDefaults.filterChipColors(
                        iconColor = color,
                        selectedLeadingIconColor = color,
                        selectedContainerColor = color.copy(alpha = 0.18f)
                    )
                )
            }
        }
    }
}

/** «12 / 40», a la derecha, debajo de un campo con límite. */
@Composable
private fun Counter(length: Int, max: Int) {
    Text("$length / $max", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
}
