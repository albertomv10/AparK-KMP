package com.albertomedina.apark.presentation.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.hide_password
import apark.composeapp.generated.resources.show_password
import com.albertomedina.apark.utils.getErrorMessage
import org.jetbrains.compose.resources.stringResource

/**
 * Un campo de contraseña con el botón de verla, y su error debajo.
 *
 * Nace con la spec 011 para los dos formularios de la cuenta, y está pensado para el borrado de
 * cuenta (1.4), que pedirá la contraseña actual igual. Login y registro tienen todavía la suya
 * escrita a mano.
 *
 * [errorKey] es una llave de las que traduce [getErrorMessage], como en el resto de formularios.
 */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorKey: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var visible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        isError = errorKey != null,
        supportingText = errorKey?.let { { Text(getErrorMessage(it)) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = keyboardActions,
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = stringResource(if (visible) Res.string.hide_password else Res.string.show_password)
                )
            }
        }
    )
}
