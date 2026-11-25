package com.albertomedina.apark.presentation.editvehicle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.add_vehicle_save
import apark.composeapp.generated.resources.back
import apark.composeapp.generated.resources.edit_vehicle_error
import apark.composeapp.generated.resources.edit_vehicle_title
import com.albertomedina.apark.presentation.components.StandardAparKButton
import com.albertomedina.apark.presentation.vehicleform.VehicleForm
import com.albertomedina.apark.utils.SnackbarMessage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Editar un vehículo (spec 015): el mismo formulario que al crearlo, con «Más datos» ya abierto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVehicleScreen(
    vehicleId: String,
    viewModel: EditVehicleViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeSnackbarMessage by remember { mutableStateOf<SnackbarMessage?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val saveError = stringResource(Res.string.edit_vehicle_error)

    LaunchedEffect(vehicleId) {
        viewModel.onEvent(EditVehicleEvent.Load(vehicleId))
    }

    LaunchedEffect(state.closeRequested) {
        if (state.closeRequested) onBack()
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            activeSnackbarMessage = msg
            val text = if (msg.message == EditVehicleViewModel.ERROR_SAVE_KEY) saveError else msg.message
            snackbarHostState.showSnackbar(text, withDismissAction = true, duration = SnackbarDuration.Long)
            viewModel.onEvent(EditVehicleEvent.SnackBarDismissed)
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
                title = { Text(stringResource(Res.string.edit_vehicle_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        if (!state.isLoaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VehicleForm(
                state = state.form,
                catalog = state.catalog,
                onEvent = { viewModel.onEvent(EditVehicleEvent.Form(it)) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            if (state.isSaving) {
                CircularProgressIndicator()
            } else {
                StandardAparKButton(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.onEvent(EditVehicleEvent.SaveClicked)
                    }
                ) {
                    Text(stringResource(Res.string.add_vehicle_save))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
