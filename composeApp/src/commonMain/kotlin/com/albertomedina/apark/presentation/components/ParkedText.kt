package com.albertomedina.apark.presentation.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.utils.RelativeTime
import com.albertomedina.apark.utils.getRelativeTime
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Quién aparcó, visto desde quien mira (spec 016). */
sealed interface WhoParked {
    data object You : WhoParked
    data class Named(val name: String) : WhoParked
    /** Nadie a quien nombrar: quien aparcó borró su cuenta y el servidor quitó quién fue (spec 012). */
    data object Unknown : WhoParked
}

fun whoParked(location: Vehicle.LocationModel, currentUserId: String?): WhoParked {
    val user = location.user ?: return WhoParked.Unknown
    if (!currentUserId.isNullOrBlank() && user.uid == currentUserId) return WhoParked.You
    // El nombre, o el email si nunca puso uno: el mismo criterio que la lista de miembros.
    val name = user.name.takeIf { it.isNotBlank() } ?: user.email.takeIf { it.isNotBlank() }
    return name?.let(WhoParked::Named) ?: WhoParked.Unknown
}

/**
 * «Aparcado hace 5 min por Edu», en una línea (spec 016): lo que dicen la tarjeta del mapa, la lista de
 * *Vehículos* y el detalle. Sin ubicación, «Aún no aparcado». Si quien aparcó fue quien mira, «por ti».
 *
 * Una frase por tipo de tiempo, y no «Aparcado» + la hora, porque lo que va delante cambia con cada
 * uno: «hace 5 min», «hoy a las 18:45», «el 01 ene». Se refresca cada minuto.
 */
@Composable
fun ParkedText(
    location: Vehicle.LocationModel?,
    currentUserId: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = 1
) {
    val text = if (location == null || location.timestamp == 0L) {
        stringResource(Res.string.home_not_parked_yet)
    } else {
        val whenParked = parkedWhen(rememberRelativeTime(location.timestamp))
        when (val who = whoParked(location, currentUserId)) {
            WhoParked.You -> stringResource(Res.string.parked_by_you, whenParked)
            is WhoParked.Named -> stringResource(Res.string.parked_by, whenParked, who.name)
            WhoParked.Unknown -> whenParked
        }
    }
    Text(text, modifier, color = color, style = style, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun rememberRelativeTime(timestamp: Long): RelativeTime {
    // Solo sirve de gatillo: cada minuto cambia, y con él se vuelve a calcular el tiempo relativo.
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(timestamp) {
        while (true) {
            delay(60_000L)
            tick++
        }
    }
    return remember(timestamp, tick) { timestamp.getRelativeTime() }
}

@Composable
private fun parkedWhen(time: RelativeTime): String = when (time) {
    RelativeTime.JustNow -> stringResource(Res.string.parked_just_now)
    is RelativeTime.MinutesAgo -> stringResource(Res.string.parked_minutes_ago, time.minutes)
    is RelativeTime.HoursAgo -> stringResource(Res.string.parked_hours_ago, time.hours)
    is RelativeTime.TodayAt -> stringResource(Res.string.parked_today_at, time.time)
    is RelativeTime.YesterdayAt -> stringResource(Res.string.parked_yesterday_at, time.time)
    is RelativeTime.AbsoluteDate ->
        stringResource(Res.string.parked_on_date, time.day, monthOf(time.monthKey)?.let { stringResource(it) } ?: "")
}

private fun monthOf(key: String): StringResource? = when (key) {
    "month_jan" -> Res.string.month_jan
    "month_feb" -> Res.string.month_feb
    "month_mar" -> Res.string.month_mar
    "month_apr" -> Res.string.month_apr
    "month_may" -> Res.string.month_may
    "month_jun" -> Res.string.month_jun
    "month_jul" -> Res.string.month_jul
    "month_aug" -> Res.string.month_aug
    "month_sep" -> Res.string.month_sep
    "month_oct" -> Res.string.month_oct
    "month_nov" -> Res.string.month_nov
    "month_dec" -> Res.string.month_dec
    else -> null
}
