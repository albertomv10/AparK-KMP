package com.albertomedina.apark.presentation.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.change_password_confirm_label
import apark.composeapp.generated.resources.change_password_done_body
import apark.composeapp.generated.resources.change_password_done_title
import apark.composeapp.generated.resources.change_password_new_label
import apark.composeapp.generated.resources.change_password_submit
import apark.composeapp.generated.resources.change_password_title
import apark.composeapp.generated.resources.current_password_label
import com.albertomedina.apark.presentation.components.PasswordField
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Cambiar la contraseña: la actual, la nueva y la nueva otra vez. Tres campos con el teclado abierto
 * es por lo que esto es una pantalla y no un diálogo.
 */
@Composable
fun ChangePasswordScreen(
    viewModel: ChangePasswordViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    AccountFormScaffold(title = stringResource(Res.string.change_password_title), onBack = onBack) {
        if (state.done) {
            AccountFormDone(
                icon = Icons.Outlined.CheckCircle,
                title = stringResource(Res.string.change_password_done_title),
                body = stringResource(Res.string.change_password_done_body),
                onDone = onBack
            )
        } else {
            PasswordField(
                value = state.current,
                onValueChange = { viewModel.onEvent(ChangePasswordEvent.CurrentChanged(it)) },
                label = stringResource(Res.string.current_password_label),
                modifier = Modifier.fillMaxWidth(),
                errorKey = state.currentError,
                enabled = !state.isSubmitting
            )

            PasswordField(
                value = state.new,
                onValueChange = { viewModel.onEvent(ChangePasswordEvent.NewChanged(it)) },
                label = stringResource(Res.string.change_password_new_label),
                modifier = Modifier.fillMaxWidth(),
                errorKey = state.newError,
                enabled = !state.isSubmitting
            )

            PasswordField(
                value = state.confirm,
                onValueChange = { viewModel.onEvent(ChangePasswordEvent.ConfirmChanged(it)) },
                label = stringResource(Res.string.change_password_confirm_label),
                modifier = Modifier.fillMaxWidth(),
                errorKey = state.confirmError,
                enabled = !state.isSubmitting,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { viewModel.onEvent(ChangePasswordEvent.SubmitClicked) })
            )

            GeneralError(state.generalError)

            SubmitButton(
                text = stringResource(Res.string.change_password_submit),
                isSubmitting = state.isSubmitting,
                onClick = { viewModel.onEvent(ChangePasswordEvent.SubmitClicked) }
            )
        }
    }
}
