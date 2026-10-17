package com.albertomedina.apark.presentation.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.settings.ThemeMode
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Cuenta: quién eres y cómo se comporta la app.
 *
 * Una sola pantalla y no «Cuenta → Ajustes»: para cinco opciones, una pantalla intermedia es
 * ceremonia. Cuando las notificaciones traigan varios interruptores, esas sí abrirán su subpantalla.
 * Ver `docs/NAVIGATION.md`.
 *
 * Aquí vive ahora el cierre de sesión, que estaba en un menú de tres puntos sobre el mapa: es una
 * acción de cuenta, no de mapa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel = koinViewModel(),
    onSignedOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) {
            onSignedOut()
            viewModel.onEvent(AccountEvent.SignOutHandled)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.tab_account)) }) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier.fillMaxWidth().padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(text = state.email, style = MaterialTheme.typography.titleMedium)
            }

            HorizontalDivider()

            Text(
                text = stringResource(Res.string.account_theme),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp)
            )

            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = state.themeMode == mode,
                                onClick = { viewModel.onEvent(AccountEvent.ThemeSelected(mode)) },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = state.themeMode == mode, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(stringResource(mode.label()))
                    }
                }
            }

            HorizontalDivider(Modifier.padding(top = 12.dp))

            if (state.isSigningOut) {
                Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) { CircularProgressIndicator() }
            } else {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = false,
                            onClick = { viewModel.onEvent(AccountEvent.SignOutClicked) },
                            role = Role.Button
                        )
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(Res.string.account_sign_out), color = MaterialTheme.colorScheme.error)
                }
            }

            if (state.error) {
                Text(
                    text = stringResource(Res.string.error_sign_out),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> Res.string.account_theme_system
    ThemeMode.LIGHT -> Res.string.account_theme_light
    ThemeMode.DARK -> Res.string.account_theme_dark
}
