package com.albertomedina.apark.presentation.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.month_apr
import apark.composeapp.generated.resources.month_aug
import apark.composeapp.generated.resources.month_dec
import apark.composeapp.generated.resources.month_feb
import apark.composeapp.generated.resources.month_jan
import apark.composeapp.generated.resources.month_jul
import apark.composeapp.generated.resources.month_jun
import apark.composeapp.generated.resources.month_mar
import apark.composeapp.generated.resources.month_may
import apark.composeapp.generated.resources.month_nov
import apark.composeapp.generated.resources.month_oct
import apark.composeapp.generated.resources.month_sep
import apark.composeapp.generated.resources.time_ago_hours
import apark.composeapp.generated.resources.time_ago_minutes
import apark.composeapp.generated.resources.time_less_than_minute
import apark.composeapp.generated.resources.time_yesterday
import com.albertomedina.apark.utils.RelativeTime
import com.albertomedina.apark.utils.getRelativeTime
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
    val relativeTime = remember(timestamp, refreshTrigger) {
        timestamp?.getRelativeTime() ?: ""
    }
    val timeLabel = when (relativeTime) {
        is RelativeTime.JustNow -> stringResource(Res.string.time_less_than_minute)
        is RelativeTime.MinutesAgo -> stringResource(Res.string.time_ago_minutes, relativeTime.minutes)
        is RelativeTime.HoursAgo -> stringResource(Res.string.time_ago_hours, relativeTime.hours)
        is RelativeTime.YesterdayAt -> stringResource(Res.string.time_yesterday, relativeTime.time)
        is RelativeTime.TodayAt -> relativeTime.time
        is RelativeTime.AbsoluteDate -> {
            // Aquí resolvemos la clave del mes dinámicamente
            val month = when(relativeTime.monthKey) {
                "month_jan" -> stringResource(Res.string.month_jan)
                "month_feb" -> stringResource(Res.string.month_feb)
                "month_mar" -> stringResource(Res.string.month_mar)
                "month_apr" -> stringResource(Res.string.month_apr)
                "month_may" -> stringResource(Res.string.month_may)
                "month_jun" -> stringResource(Res.string.month_jun)
                "month_jul" -> stringResource(Res.string.month_jul)
                "month_aug" -> stringResource(Res.string.month_aug)
                "month_sep" -> stringResource(Res.string.month_sep)
                "month_oct" -> stringResource(Res.string.month_oct)
                "month_nov" -> stringResource(Res.string.month_nov)
                "month_dec" -> stringResource(Res.string.month_dec)
                else -> ""
            }
            "${relativeTime.day} $month"
        }

        else -> ""
    }


    val message = stringResource(text, timeLabel)

    Text(
        text = message,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = style,
        color = color
    )
}