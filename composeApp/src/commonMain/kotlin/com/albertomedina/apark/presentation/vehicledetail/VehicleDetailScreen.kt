package com.albertomedina.apark.presentation.vehicledetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.presentation.components.StandardAparKButton
import com.albertomedina.apark.utils.ShareTextHandler
import com.albertomedina.apark.utils.SnackbarMessage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    vehicleId: String,
    viewModel: VehicleDetailViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeSnackbarMessage by remember { mutableStateOf<SnackbarMessage?>(null) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var shareTrigger by remember { mutableIntStateOf(0) }
    var shareText by remember { mutableStateOf("") }

    val errorMessage = stringResource(Res.string.share_vehicle_error)
    val copiedMessage = stringResource(Res.string.share_vehicle_copied)

    LaunchedEffect(vehicleId) {
        viewModel.onEvent(VehicleDetailEvent.Load(vehicleId))
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            activeSnackbarMessage = msg
            val text = when (msg.message) {
                VehicleDetailViewModel.ERROR_INVITE_KEY -> errorMessage
                else -> msg.message
            }
            snackbarHostState.showSnackbar(text, withDismissAction = true, duration = SnackbarDuration.Long)
            viewModel.onEvent(VehicleDetailEvent.SnackBarDismissed)
        }
    }

    ShareTextHandler(trigger = shareTrigger, text = shareText, onShared = {})

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
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.add_vehicle_back)
                        )
                    }
                },
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
                return@Column
            }

            Text(
                text = state.vehicleName,
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Sharing is the owner's alone; a member sees nothing here for now.
            if (state.isOwner) {
                if (state.isCreatingInvite) {
                    CircularProgressIndicator()
                } else {
                    StandardAparKButton(onClick = { viewModel.onEvent(VehicleDetailEvent.ShareClicked) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.fillMaxWidth(0.05f))
                            Text(stringResource(Res.string.share_vehicle_action))
                        }
                    }
                }
            }
        }
    }

    state.invite?.let { invite ->
        val shareMessage = stringResource(Res.string.share_vehicle_message, state.vehicleName, invite.code)
        // Feedback lives on the button itself: a snackbar would render behind the dialog's scrim
        // and the copy would look like it did nothing.
        var justCopied by remember(invite.code) { mutableStateOf(false) }

        LaunchedEffect(justCopied) {
            if (justCopied) {
                delay(2000)
                justCopied = false
            }
        }

        AlertDialog(
            onDismissRequest = { viewModel.onEvent(VehicleDetailEvent.InviteDismissed) },
            title = { Text(stringResource(Res.string.share_vehicle_dialog_title)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = invite.code,
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
                TextButton(onClick = {
                    shareText = shareMessage
                    shareTrigger++
                }) {
                    Text(stringResource(Res.string.share_vehicle_send))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(invite.code))
                    justCopied = true
                }) {
                    Text(
                        text = if (justCopied) copiedMessage else stringResource(Res.string.share_vehicle_copy)
                    )
                }
            }
        )
    }
}
