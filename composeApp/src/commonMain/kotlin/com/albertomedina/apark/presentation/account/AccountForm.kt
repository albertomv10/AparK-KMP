package com.albertomedina.apark.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.back
import apark.composeapp.generated.resources.done
import com.albertomedina.apark.presentation.components.StandardAparKButton
import com.albertomedina.apark.utils.getErrorMessage
import org.jetbrains.compose.resources.stringResource

/**
 * El armazón de los formularios de la cuenta: barra con «atrás», contenido que se desplaza y sube
 * con el teclado. Sin barra de pestañas, que es lo que declaran sus destinos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountFormScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/** El botón de enviar, que se convierte en un indicador mientras la petición está en marcha. */
@Composable
internal fun ColumnScope.SubmitButton(text: String, isSubmitting: Boolean, onClick: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    if (isSubmitting) {
        CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
    } else {
        StandardAparKButton(onClick = onClick) { Text(text) }
    }
}

/** Un fallo que no es de ningún campo: en el formulario mismo, junto al botón que lo provocó. */
@Composable
internal fun GeneralError(key: String?) {
    key ?: return
    Text(
        text = getErrorMessage(key),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}

/** Lo que se ve cuando la operación ha salido bien: qué ha pasado, y qué toca ahora. */
@Composable
internal fun ColumnScope.AccountFormDone(icon: ImageVector, title: String, body: String, onDone: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally),
        tint = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = title,
        // `titleLarge` y no `headlineSmall`: en la escala de este proyecto los headline son los
        // pequeños (14sp), y el título quedaba por debajo del texto que lo explica.
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))
    StandardAparKButton(onClick = onDone) { Text(stringResource(Res.string.done)) }
}
