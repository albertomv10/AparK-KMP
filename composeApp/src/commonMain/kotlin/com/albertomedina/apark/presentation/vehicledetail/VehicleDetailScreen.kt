package com.albertomedina.apark.presentation.vehicledetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.model.EmissionsLabel
import com.albertomedina.apark.domain.model.Fuel
import com.albertomedina.apark.domain.model.Powertrain
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.VehicleColor
import com.albertomedina.apark.domain.model.VehicleType
import com.albertomedina.apark.domain.model.hasDetails
import com.albertomedina.apark.presentation.vehicleform.VehicleAvatar
import com.albertomedina.apark.presentation.vehicleform.badgeColor
import com.albertomedina.apark.presentation.vehicleform.hasAvatar
import com.albertomedina.apark.presentation.vehicleform.label
import com.albertomedina.apark.presentation.vehicleform.swatch
import com.albertomedina.apark.presentation.components.SectionTitle
import com.albertomedina.apark.presentation.components.AparKConfirmDialog
import com.albertomedina.apark.presentation.components.ParkedText
import com.albertomedina.apark.presentation.components.StandardAparKButton
import com.albertomedina.apark.presentation.incidents.IncidentsSection
import com.albertomedina.apark.utils.ShareTextHandler
import com.albertomedina.apark.utils.SnackbarMessage
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * El detalle de un vehículo (spec 014), por secciones: dónde está, qué le pasa (spec 019), quién lo
 * comparte y, al final, lo que saca de él. La fase 3 añadirá las suyas —ITV, mantenimiento— entre
 * medias, sin tocar estas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    vehicleId: String,
    viewModel: VehicleDetailViewModel = koinViewModel(),
    onBack: () -> Unit,
    onShowOnMap: (String) -> Unit,
    onEdit: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeSnackbarMessage by remember { mutableStateOf<SnackbarMessage?>(null) }

    val inviteError = stringResource(Res.string.share_vehicle_error)
    val actionError = stringResource(Res.string.delete_vehicle_error)
    val memberRemoved = stringResource(Res.string.vehicle_detail_member_removed)
    val ownerTransferred = stringResource(Res.string.vehicle_detail_owner_transferred)

    LaunchedEffect(vehicleId) {
        viewModel.onEvent(VehicleDetailEvent.Load(vehicleId))
    }

    // Las incidencias (spec 019) sacan sus avisos por el mismo hueco, y el de resolver lleva «Deshacer».
    val showSnackbar: suspend (SnackbarMessage) -> SnackbarResult = { message ->
        activeSnackbarMessage = message
        snackbarHostState.showSnackbar(
            message.message,
            actionLabel = message.actionLabel,
            withDismissAction = true,
            duration = SnackbarDuration.Long
        )
    }
    // El nombre de quien reportó o resolvió sale de aquí: si ya no es miembro, no está (spec 019).
    val memberNames = (state.members as? MembersState.Loaded)?.members.orEmpty().associate { it.uid to it.displayName }

    LaunchedEffect(state.closeRequested) {
        if (state.closeRequested) onBack()
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            activeSnackbarMessage = msg
            val text = when (msg.message) {
                VehicleDetailViewModel.ERROR_INVITE_KEY -> inviteError
                VehicleDetailViewModel.ACTION_ERROR_KEY -> actionError
                VehicleDetailViewModel.MEMBER_REMOVED_KEY -> memberRemoved
                VehicleDetailViewModel.OWNER_TRANSFERRED_KEY -> ownerTransferred
                else -> msg.message
            }
            snackbarHostState.showSnackbar(text, withDismissAction = true, duration = SnackbarDuration.Long)
            viewModel.onEvent(VehicleDetailEvent.SnackBarDismissed)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                activeSnackbarMessage?.let {
                    Snackbar(
                        snackbarData = data,
                        containerColor = it.backgroundColor(),
                        contentColor = it.contentColor(),
                        actionColor = it.contentColor()
                    )
                } ?: Snackbar(snackbarData = data)
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(state.vehicleName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    // Solo el dueño edita (spec 015); al resto ni se le ofrece.
                    val vehicle = state.vehicle
                    if (state.isOwner && vehicle != null) {
                        IconButton(onClick = { onEdit(vehicle.id) }, enabled = !state.isWorking) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.edit_vehicle_title))
                        }
                    }
                }
            )
        }
    ) { padding ->
        // Va dentro del shell de pestañas: sin consumir los insets, el contenido subiría el alto
        // del indicador de inicio. Ver «A Scaffold inside the tab shell» en AGENTS.md.
        val contentModifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
        val vehicle = state.vehicle
        when {
            state.isGone -> Message(
                title = stringResource(Res.string.vehicle_detail_gone),
                body = stringResource(Res.string.vehicle_detail_gone_hint),
                onBack = onBack,
                modifier = contentModifier
            )
            state.loadFailed -> Message(
                title = stringResource(Res.string.vehicle_detail_load_error),
                body = null,
                onBack = onBack,
                modifier = contentModifier
            )
            state.isLoading || vehicle == null -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> Column(contentModifier.verticalScroll(rememberScrollState())) {
                if (state.isWorking) LinearProgressIndicator(Modifier.fillMaxWidth())
                Header(vehicle)
                LocationSection(vehicle, state.currentUserId, onShowOnMap = { onShowOnMap(vehicle.id) })
                HorizontalDivider(Modifier.padding(top = 12.dp))
                IncidentsSection(
                    vehicle = vehicle,
                    isOwner = state.isOwner,
                    names = memberNames,
                    title = { SectionTitle(stringResource(Res.string.incidents_title)) },
                    showSnackbar = showSnackbar
                )
                if (vehicle.hasDetails || state.isOwner) {
                    HorizontalDivider(Modifier.padding(top = 12.dp))
                    DataSection(vehicle, isOwner = state.isOwner, onEdit = { onEdit(vehicle.id) })
                }
                HorizontalDivider(Modifier.padding(top = 12.dp))
                MembersSection(state, viewModel::onEvent)
                HorizontalDivider(Modifier.padding(top = 12.dp))
                if (state.isOwner) {
                    DestructiveRow(Icons.Outlined.DeleteForever, stringResource(Res.string.delete_vehicle_action), enabled = !state.isWorking) {
                        viewModel.onEvent(VehicleDetailEvent.ActionRequested(DetailAction.DeleteVehicle))
                    }
                } else {
                    DestructiveRow(Icons.AutoMirrored.Filled.Logout, stringResource(Res.string.remove_vehicle_action), enabled = !state.isWorking) {
                        viewModel.onEvent(VehicleDetailEvent.ActionRequested(DetailAction.Leave))
                    }
                }
            }
        }
    }

    state.pendingAction?.let { action -> ConfirmAction(action, state.vehicleName, viewModel::onEvent) }
    state.invite?.let { invite -> InviteDialog(invite.code, state.vehicleName, viewModel::onEvent) }
}

/**
 * El nombre ya va en la barra, que sigue a la vista al desplazarse: aquí, el tipo y el color —como en
 * la tarjeta del mapa— y la matrícula. Lo que no hay no deja hueco.
 */
@Composable
private fun Header(vehicle: Vehicle) {
    if (vehicle.licensePlate.isBlank() && !vehicle.hasAvatar) return
    Row(
        Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (vehicle.hasAvatar) {
            VehicleAvatar(vehicle, size = 48.dp)
            if (vehicle.licensePlate.isNotBlank()) Spacer(Modifier.width(16.dp))
        }
        if (vehicle.licensePlate.isNotBlank()) {
            Text(
                vehicle.licensePlate,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Los datos del vehículo (spec 015), como una ficha: solo las filas que tienen algo (criterio 9). Sin
 * ninguno, el dueño ve cómo añadirlos; los demás, nada, y la sección ni aparece.
 */
@Composable
private fun DataSection(vehicle: Vehicle, isOwner: Boolean, onEdit: () -> Unit) {
    SectionTitle(stringResource(Res.string.vehicle_detail_data_title))
    if (!vehicle.hasDetails) {
        ListItem(
            headlineContent = { Text(stringResource(Res.string.vehicle_detail_add_data)) },
            supportingContent = { Text(stringResource(Res.string.vehicle_detail_add_data_hint)) },
            leadingContent = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .selectable(selected = false, enabled = isOwner, onClick = onEdit, role = Role.Button)
        )
        return
    }

    Column(Modifier.padding(horizontal = 24.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VehicleType.fromCode(vehicle.type)?.let { DataRow(stringResource(Res.string.vehicle_form_type), stringResource(it.label)) }
        vehicle.make.takeIf { it.isNotBlank() }?.let { DataRow(stringResource(Res.string.vehicle_form_make), it) }
        vehicle.model.takeIf { it.isNotBlank() }?.let { DataRow(stringResource(Res.string.vehicle_form_model), it) }
        VehicleColor.fromCode(vehicle.color)?.let { color ->
            DataRow(stringResource(Res.string.vehicle_form_color), stringResource(color.label)) {
                Box(
                    Modifier
                        .size(14.dp)
                        .background(color.swatch, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
            }
        }
        Powertrain.fromCode(vehicle.powertrain)?.let { DataRow(stringResource(Res.string.vehicle_form_powertrain), stringResource(it.label)) }
        Fuel.fromCode(vehicle.fuel)?.let { DataRow(stringResource(Res.string.vehicle_form_fuel), stringResource(it.label)) }
        EmissionsLabel.fromCode(vehicle.emissionsLabel)?.let { label ->
            DataRow(stringResource(Res.string.vehicle_form_emissions), stringResource(label.label)) {
                label.badgeColor?.let { Box(Modifier.size(14.dp).background(it, CircleShape)) }
            }
        }
    }
}

/** Una fila de la ficha: qué es a la izquierda, su valor a la derecha, con su muestra si la tiene. */
@Composable
private fun DataRow(label: String, value: String, marker: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Row(Modifier.weight(0.55f), verticalAlignment = Alignment.CenterVertically) {
            if (marker != null) {
                marker()
                Spacer(Modifier.width(8.dp))
            }
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun LocationSection(vehicle: Vehicle, currentUserId: String?, onShowOnMap: () -> Unit) {
    SectionTitle(stringResource(Res.string.vehicle_detail_location_title))
    Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val location = vehicle.lastLocation
        // La misma frase que la tarjeta y la lista (spec 016); aquí puede ocupar dos líneas.
        ParkedText(location, currentUserId, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
        if (location == null || location.timestamp == 0L) return@Column
        OutlinedButton(onClick = onShowOnMap, modifier = Modifier.padding(top = 8.dp)) {
            Icon(Icons.Default.Map, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.vehicle_detail_show_on_map))
        }
    }
}

@Composable
private fun ColumnScope.MembersSection(state: VehicleDetailUiState, onEvent: (VehicleDetailEvent) -> Unit) {
    SectionTitle(stringResource(Res.string.vehicle_detail_members_title))
    when (val members = state.members) {
        MembersState.Loading -> CircularProgressIndicator(Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
        MembersState.Error -> Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(Res.string.vehicle_detail_members_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { onEvent(VehicleDetailEvent.RetryMembers) }) { Text(stringResource(Res.string.retry)) }
        }
        is MembersState.Loaded -> {
            val ownerId = state.vehicle?.ownerId.orEmpty()
            memberRows(members.members, ownerId, state.currentUserId).forEach { row ->
                MemberItem(row, canManage = state.isOwner && !row.isMe && !state.isWorking, onEvent = onEvent)
            }
        }
    }

    if (state.isOwner) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            if (state.isCreatingInvite) {
                CircularProgressIndicator()
            } else {
                StandardAparKButton(onClick = { onEvent(VehicleDetailEvent.ShareClicked) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.share_vehicle_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberItem(row: MemberRow, canManage: Boolean, onEvent: (VehicleDetailEvent) -> Unit) {
    val name = row.member.displayName.ifBlank { stringResource(Res.string.vehicle_detail_member_unnamed) }
    val badges = listOfNotNull(
        stringResource(Res.string.vehicle_detail_member_owner).takeIf { row.isOwner },
        stringResource(Res.string.vehicle_detail_member_you).takeIf { row.isMe }
    )
    var menuOpen by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (badges.isEmpty()) null else {
            { Text(badges.joinToString(" · "), color = MaterialTheme.colorScheme.primary) }
        },
        trailingContent = if (!canManage) null else {
            {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(Res.string.vehicle_detail_member_options, name)
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.vehicle_detail_make_owner)) },
                            onClick = {
                                menuOpen = false
                                onEvent(VehicleDetailEvent.ActionRequested(DetailAction.TransferTo(row.member)))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.vehicle_detail_remove_member), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuOpen = false
                                onEvent(VehicleDetailEvent.ActionRequested(DetailAction.RemoveMember(row.member)))
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun ConfirmAction(action: DetailAction, vehicleName: String, onEvent: (VehicleDetailEvent) -> Unit) {
    val unnamed = stringResource(Res.string.vehicle_detail_member_unnamed)
    val (title, text, confirm) = when (action) {
        is DetailAction.TransferTo -> {
            val name = action.member.displayName.ifBlank { unnamed }
            Triple(
                stringResource(Res.string.vehicle_detail_transfer_title, name),
                stringResource(Res.string.vehicle_detail_transfer_message, name, vehicleName),
                stringResource(Res.string.vehicle_detail_transfer_confirm)
            )
        }
        is DetailAction.RemoveMember -> {
            val name = action.member.displayName.ifBlank { unnamed }
            Triple(
                stringResource(Res.string.vehicle_detail_remove_title, name),
                stringResource(Res.string.vehicle_detail_remove_message, name, vehicleName),
                stringResource(Res.string.vehicle_detail_remove_confirm)
            )
        }
        DetailAction.DeleteVehicle -> Triple(
            stringResource(Res.string.delete_vehicle_confirm_title, vehicleName),
            stringResource(Res.string.delete_vehicle_confirm_message),
            stringResource(Res.string.delete_vehicle_confirm_action)
        )
        DetailAction.Leave -> Triple(
            stringResource(Res.string.remove_vehicle_confirm_title, vehicleName),
            stringResource(Res.string.remove_vehicle_confirm_message),
            stringResource(Res.string.remove_vehicle_confirm_action)
        )
    }
    AparKConfirmDialog(
        title = title,
        text = text,
        confirmLabel = confirm,
        dismissLabel = stringResource(Res.string.cancel),
        onConfirm = { onEvent(VehicleDetailEvent.ActionConfirmed) },
        onDismiss = { onEvent(VehicleDetailEvent.ActionDismissed) },
        // Pasar la propiedad no destruye nada, pero no se puede deshacer desde este lado.
        isDestructive = action !is DetailAction.TransferTo
    )
}

/** Estaba y ya no está, o no ha podido cargarse: lo dice, y ofrece volver. */
@Composable
private fun Message(title: String, body: String?, onBack: () -> Unit, modifier: Modifier) {
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(24.dp))
        StandardAparKButton(onClick = onBack) { Text(stringResource(Res.string.back)) }
    }
}

/** Lo que saca de algo: en rojo, con su icono, como en *Cuenta*. */
@Composable
private fun DestructiveRow(icon: ImageVector, text: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = false, enabled = enabled, onClick = onClick, role = Role.Button)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(16.dp))
        Text(text, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun InviteDialog(code: String, vehicleName: String, onEvent: (VehicleDetailEvent) -> Unit) {
    val clipboard = LocalClipboardManager.current
    val copiedMessage = stringResource(Res.string.share_vehicle_copied)
    val shareMessage = stringResource(Res.string.share_vehicle_message, vehicleName, code)
    var shareTrigger by remember { mutableIntStateOf(0) }
    // El aviso vive en el propio botón: un snackbar quedaría detrás del velo del diálogo, y copiar
    // parecería no hacer nada.
    var justCopied by remember(code) { mutableStateOf(false) }

    ShareTextHandler(trigger = shareTrigger, text = shareMessage, onShared = {})

    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(2000)
            justCopied = false
        }
    }

    AlertDialog(
        onDismissRequest = { onEvent(VehicleDetailEvent.InviteDismissed) },
        title = { Text(stringResource(Res.string.share_vehicle_dialog_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.share_vehicle_expiry),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { shareTrigger++ }) { Text(stringResource(Res.string.share_vehicle_send)) }
        },
        dismissButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(code))
                justCopied = true
            }) {
                Text(if (justCopied) copiedMessage else stringResource(Res.string.share_vehicle_copy))
            }
        }
    )
}
