package com.albertomedina.apark.presentation.defaultplace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.presentation.components.PlacePickerMap
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Elegir la ubicación predeterminada de un vehículo (spec 024): se mueve el mapa hasta que la plaza
 * queda bajo el marcador del centro, se le pone nombre y se guarda. Si ya la tiene, también se quita.
 *
 * El mapa ocupa todo lo que deja el panel de abajo, y no queda debajo de él: así el centro del mapa es
 * el centro de lo que se ve, que es donde está el marcador.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultPlaceScreen(
    vehicleId: String,
    viewModel: DefaultPlaceViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val onEvent = viewModel::onEvent

    LaunchedEffect(vehicleId) { onEvent(DefaultPlaceEvent.Load(vehicleId)) }
    LaunchedEffect(state.closeRequested) { if (state.closeRequested) onBack() }

    val defaultName = stringResource(Res.string.default_place_default_name)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.default_place_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val initial = state.initial
                if (initial == null) {
                    CircularProgressIndicator()
                } else {
                    // Nace una vez, donde dice el ViewModel; después lo mueve quien mira.
                    key(initial) {
                        PlacePickerMap(
                            modifier = Modifier.fillMaxSize(),
                            initial = initial,
                            initialZoom = state.initialZoom,
                            onCenterChanged = { onEvent(DefaultPlaceEvent.CenterChanged(it)) }
                        )
                    }
                    CenterPin()
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        tonalElevation = 2.dp,
                        modifier = Modifier.align(Alignment.TopCenter).padding(12.dp)
                    ) {
                        Text(
                            stringResource(Res.string.default_place_hint),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val name = state.name ?: defaultName
                OutlinedTextField(
                    value = name,
                    onValueChange = { onEvent(DefaultPlaceEvent.NameChanged(it)) },
                    label = { Text(stringResource(Res.string.default_place_name)) },
                    singleLine = true,
                    supportingText = { Text("${name.length} / ${Vehicle.DefaultPlace.NAME_MAX_LENGTH}") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.existing != null) {
                        OutlinedButton(
                            onClick = { onEvent(DefaultPlaceEvent.RemoveClicked) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(Res.string.default_place_remove)) }
                    }
                    Button(
                        onClick = { onEvent(DefaultPlaceEvent.SaveClicked(defaultName)) },
                        enabled = state.center != null,
                        modifier = Modifier.weight(1f)
                    ) { Text(stringResource(Res.string.default_place_save)) }
                }
            }
        }
    }
}

/**
 * El marcador fijo del centro. Dibujado aquí, encima del mapa, y no dentro de él: así es el mismo en
 * Android y en iOS. La punta del icono está a 22 de sus 24 unidades, 44 dp de 48, así que se sube 20
 * dp para que la punta, y no el centro del icono, caiga en el centro del mapa.
 */
@Composable
private fun CenterPin() {
    Icon(
        Icons.Filled.Place,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(48.dp).offset(y = (-20).dp)
    )
}
