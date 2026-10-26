package com.albertomedina.apark.presentation.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.change_email_intro
import apark.composeapp.generated.resources.change_email_new_label
import apark.composeapp.generated.resources.change_email_sent_body
import apark.composeapp.generated.resources.change_email_sent_title
import apark.composeapp.generated.resources.change_email_submit
import apark.composeapp.generated.resources.change_email_title
import apark.composeapp.generated.resources.current_password_label
import com.albertomedina.apark.presentation.components.PasswordField
import com.albertomedina.apark.utils.getErrorMessage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Cambiar el email. La contraseña actual se pide **siempre**: Firebase exige un login reciente y no
 * deja saber de antemano si lo es (spec 011, criterio 2).
 *
 * Al enviarlo no cambia nada todavía, y la confirmación lo dice tal cual: el email cambia al abrir
 * el enlace, y entonces Firebase cierra la sesión (criterio 3).
 */
@Composable
fun ChangeEmailScreen(
    viewModel: ChangeEmailViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AccountFormScaffold(title = stringResource(Res.string.change_email_title), onBack = onBack) {
        val sentTo = state.sentTo
        if (sentTo != null) {
            AccountFormDone(
                icon = Icons.Outlined.MarkEmailRead,
                title = stringResource(Res.string.change_email_sent_title),
                body = stringResource(Res.string.change_email_sent_body, sentTo),
                onDone = onBack
            )
        } else {
            Text(
                text = stringResource(Res.string.change_email_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = state.newEmail,
                onValueChange = { viewModel.onEvent(ChangeEmailEvent.EmailChanged(it)) },
                label = { Text(stringResource(Res.string.change_email_new_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isSubmitting,
                isError = state.emailError != null,
                supportingText = state.emailError?.let { { Text(getErrorMessage(it)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )

            PasswordField(
                value = state.password,
                onValueChange = { viewModel.onEvent(ChangeEmailEvent.PasswordChanged(it)) },
                label = stringResource(Res.string.current_password_label),
                modifier = Modifier.fillMaxWidth(),
                errorKey = state.passwordError,
                enabled = !state.isSubmitting,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { viewModel.onEvent(ChangeEmailEvent.SubmitClicked) })
            )

            GeneralError(state.generalError)

            SubmitButton(
                text = stringResource(Res.string.change_email_submit),
                isSubmitting = state.isSubmitting,
                onClick = { viewModel.onEvent(ChangeEmailEvent.SubmitClicked) }
            )
        }
    }
}
