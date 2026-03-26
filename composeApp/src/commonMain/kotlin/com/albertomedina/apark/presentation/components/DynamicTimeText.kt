package com.albertomedina.apark.presentation.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.albertomedina.apark.utils.toSmartTimeLabel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DynamicTimeText(
    timestamp: Long?,
    text: StringResource,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified


) {
    // Este estado sirve únicamente como gatillo (trigger)
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(timestamp) {
        while (true) {
            // Actualizamos el gatillo cada 60 segundos (o el tiempo que prefieras)
            delay(60_000L)
            refreshTrigger++
        }
    }

    // Al leer refreshTrigger, Compose sabe que debe volver a ejecutar este bloque
    // cuando cambie, lo que volverá a llamar a toSmartTimeLabel() con la hora actual real.
    val timeLabel = remember(timestamp, refreshTrigger) {
        timestamp?.toSmartTimeLabel() ?: ""
    }

    val message = stringResource(text, timeLabel)

    Text(
        text = message,
        style = style,
        color = color
    )
}