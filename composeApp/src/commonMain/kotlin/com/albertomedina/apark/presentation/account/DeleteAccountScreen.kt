package com.albertomedina.apark.presentation.account

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.current_password_label
import apark.composeapp.generated.resources.delete_account_apple_unavailable
import apark.composeapp.generated.resources.delete_account_deleted_header
import apark.composeapp.generated.resources.delete_account_done_body
import apark.composeapp.generated.resources.delete_account_done_title
import apark.composeapp.generated.resources.delete_account_google_button
import apark.composeapp.generated.resources.delete_account_google_hint
import apark.composeapp.generated.resources.delete_account_handed_over_header
import apark.composeapp.generated.resources.delete_account_intro
import apark.composeapp.generated.resources.delete_account_left_header
import apark.composeapp.generated.resources.delete_account_no_vehicles
import apark.composeapp.generated.resources.delete_account_password_hint
import apark.composeapp.generated.resources.delete_account_submit
import apark.composeapp.generated.resources.delete_account_title
import com.albertomedina.apark.domain.auth.SignInProvider
import com.albertomedina.apark.domain.usecase.AccountDeletionImpact
import com.albertomedina.apark.presentation.components.GoogleSignInButton
import com.albertomedina.apark.presentation.components.PasswordField
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Eliminar la cuenta (spec 012). Primero dice qué se va a perder; después pide que la persona
 * demuestre que es ella, de la forma que corresponda a cómo entró.
 *
 * Una pantalla y no un diálogo, por lo mismo que cambiar el email: hay una lista que leer y, para
 * las cuentas con contraseña, un campo que rellenar.
 */
@Composable
fun DeleteAccountScreen(
    viewModel: DeleteAccountViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AccountFormScaffold(title = stringResource(Res.string.delete_account_title), onBack = onBack) {
        if (state.done) {
            AccountFormDone(
                icon = Icons.Outlined.CheckCircle,
                title = stringResource(Res.string.delete_account_done_title),
                body = stringResource(Res.string.delete_account_done_body),
                onDone = { viewModel.onEvent(DeleteAccountEvent.DoneAcknowledged) }
            )
        } else {
            Text(
                text = stringResource(Res.string.delete_account_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )

            val impact = state.impact
            if (impact == null) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            } else {
                ImpactSummary(impact)
            }

            Spacer(Modifier.height(8.dp))

            when (state.provider) {
                SignInProvider.PASSWORD -> {
                    Hint(stringResource(Res.string.delete_account_password_hint))
                    PasswordField(
                        value = state.password,
                        onValueChange = { viewModel.onEvent(DeleteAccountEvent.PasswordChanged(it)) },
                        label = stringResource(Res.string.current_password_label),
                        modifier = Modifier.fillMaxWidth(),
                        errorKey = state.passwordError,
                        enabled = !state.isSubmitting,
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(onDone = { viewModel.onEvent(DeleteAccountEvent.SubmitPassword) })
                    )
                    GeneralError(state.generalError)
                    SubmitButton(
                        text = stringResource(Res.string.delete_account_submit),
                        isSubmitting = state.isSubmitting,
                        color = MaterialTheme.colorScheme.error,
                        onClick = { viewModel.onEvent(DeleteAccountEvent.SubmitPassword) }
                    )
                }

                SignInProvider.GOOGLE -> {
                    Hint(stringResource(Res.string.delete_account_google_hint))
                    GeneralError(state.generalError)
                    if (state.isSubmitting) {
                        CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        GoogleSignInButton(
                            modifier = Modifier.fillMaxWidth(),
                            buttonText = stringResource(Res.string.delete_account_google_button),
                            onTokenReceived = { idToken, accessToken ->
                                viewModel.onEvent(DeleteAccountEvent.GoogleTokenReceived(idToken, accessToken))
                            },
                            onError = { viewModel.onEvent(DeleteAccountEvent.GoogleFailed(it)) }
                        )
                    }
                }

                // Hoy no puede haber cuentas así: falta el entitlement, que exige la cuenta de pago.
                SignInProvider.APPLE -> Hint(stringResource(Res.string.delete_account_apple_unavailable))
            }
        }
    }
}

@Composable
private fun ImpactSummary(impact: AccountDeletionImpact) {
    if (impact.deleted.isEmpty() && impact.handedOver.isEmpty() && impact.left.isEmpty()) {
        Hint(stringResource(Res.string.delete_account_no_vehicles))
        return
    }
    ImpactGroup(Res.string.delete_account_deleted_header, impact.deleted)
    ImpactGroup(Res.string.delete_account_handed_over_header, impact.handedOver)
    ImpactGroup(Res.string.delete_account_left_header, impact.left)
}

/** Un grupo vacío no se enseña: «Se borrarán» seguido de nada es ruido. */
@Composable
private fun ImpactGroup(header: StringResource, names: List<String>) {
    if (names.isEmpty()) return
    Text(
        text = stringResource(header),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
    names.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
}

@Composable
private fun Hint(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
