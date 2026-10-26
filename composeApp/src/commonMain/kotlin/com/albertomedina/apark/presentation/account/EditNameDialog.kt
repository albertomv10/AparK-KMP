package com.albertomedina.apark.presentation.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.account_name_dialog_hint
import apark.composeapp.generated.resources.account_name_dialog_title
import apark.composeapp.generated.resources.account_name_label
import apark.composeapp.generated.resources.cancel
import apark.composeapp.generated.resources.save
import com.albertomedina.apark.domain.usecase.USER_NAME_MAX_LENGTH
import com.albertomedina.apark.utils.getErrorMessage
import org.jetbrains.compose.resources.stringResource

/**
 * Cambiar el nombre: un diálogo y no una pantalla, porque es un solo campo y no pide contraseña.
 *
 * «Guardar» se apaga mientras no haya nada que guardar, y el contador avisa del tope antes de
 * llegar a él, en lugar de un error después.
 */
@Composable
fun EditNameDialog(
    state: NameDialogState,
    currentName: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val canSave = !state.isSaving && state.draft.isNotBlank() && state.draft.trim() != currentName

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.account_name_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.account_name_dialog_hint),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(Res.string.account_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isSaving,
                    isError = state.errorKey != null,
                    supportingText = {
                        Text(state.errorKey?.let { getErrorMessage(it) } ?: "${state.draft.length}/$USER_NAME_MAX_LENGTH")
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (canSave) onSave() })
                )
            }
        },
        confirmButton = {
            if (state.isSaving) {
                CircularProgressIndicator(Modifier.size(24.dp))
            } else {
                TextButton(onClick = onSave, enabled = canSave) { Text(stringResource(Res.string.save)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        }
    )
}
