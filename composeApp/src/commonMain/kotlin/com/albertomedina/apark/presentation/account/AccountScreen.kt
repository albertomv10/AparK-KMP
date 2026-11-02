package com.albertomedina.apark.presentation.account

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.auth.SignInProvider
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
 *
 * Desde la spec 011 la cuenta se puede editar. El nombre se cambia en un diálogo, porque es una
 * edición rápida. El email y la contraseña llevan a su propia pantalla, y solo si la cuenta tiene
 * contraseña: a quien entró con Google o Apple se le explica de dónde viene su identidad, en vez de
 * esconderle las opciones sin decir nada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel = koinViewModel(),
    onNavigateToChangeEmail: () -> Unit,
    onNavigateToChangePassword: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    state.nameDialog?.let { dialog ->
        EditNameDialog(
            state = dialog,
            currentName = state.name,
            onNameChange = { viewModel.onEvent(AccountEvent.NameDraftChanged(it)) },
            onSave = { viewModel.onEvent(AccountEvent.NameSaveClicked) },
            onDismiss = { viewModel.onEvent(AccountEvent.NameDialogDismissed) }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.tab_account)) }) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onEvent(AccountEvent.EditNameClicked) }
                    .padding(start = 24.dp, end = 12.dp, top = 24.dp, bottom = 24.dp),
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
                Column(Modifier.weight(1f)) {
                    // Sin nombre, las tarjetas enseñan el email a los demás miembros: invitar a
                    // ponerlo es más útil que dejar el hueco vacío.
                    if (state.name.isBlank()) {
                        Text(
                            text = stringResource(Res.string.account_name_placeholder),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = state.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = state.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { viewModel.onEvent(AccountEvent.EditNameClicked) }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.account_edit_name))
                }
            }

            HorizontalDivider()

            SectionTitle(stringResource(Res.string.account_access))

            if (state.provider.canChangeCredentials) {
                NavigationRow(
                    title = stringResource(Res.string.account_change_email),
                    subtitle = state.email,
                    onClick = onNavigateToChangeEmail
                )
                NavigationRow(
                    title = stringResource(Res.string.account_change_password),
                    subtitle = "••••••••",
                    onClick = onNavigateToChangePassword
                )
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = stringResource(
                            if (state.provider == SignInProvider.APPLE) Res.string.account_managed_by_apple
                            else Res.string.account_managed_by_google
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(Modifier.padding(top = 12.dp))

            SectionTitle(stringResource(Res.string.account_theme))

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

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp)
    )
}

/** Una fila que lleva a otra pantalla: el valor actual debajo, y la flecha a la derecha. */
@Composable
private fun NavigationRow(title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp)
    )
}

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> Res.string.account_theme_system
    ThemeMode.LIGHT -> Res.string.account_theme_light
    ThemeMode.DARK -> Res.string.account_theme_dark
}
