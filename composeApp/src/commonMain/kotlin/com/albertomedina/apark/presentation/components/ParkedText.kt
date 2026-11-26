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

/**
 * Quién hizo algo —aparcar, reportar o resolver una incidencia—, visto desde quien mira (specs 016 y
 * 019): «por ti», «por Edu», o nada si no hay a quién nombrar.
 */
sealed interface Who {
    data object You : Who
    data class Named(val name: String) : Who
    /** Nadie a quien nombrar: borró su cuenta, o ya no es miembro y no se sabe su nombre. */
    data object Unknown : Who
}

fun whoParked(location: Vehicle.LocationModel, currentUserId: String?): Who {
    val user = location.user ?: return Who.Unknown
    if (!currentUserId.isNullOrBlank() && user.uid == currentUserId) return Who.You
    // El nombre, o el email si nunca puso uno: el mismo criterio que la lista de miembros.
    val name = user.name.takeIf { it.isNotBlank() } ?: user.email.takeIf { it.isNotBlank() }
    return name?.let(Who::Named) ?: Who.Unknown
}

/**
 * Quién es un uid, con los nombres de los miembros del vehículo (spec 019). Las incidencias solo
 * guardan el uid: si ya no está entre los miembros —se fue, o borró su cuenta—, no se nombra a nadie.
 */
fun whoIs(uid: String, currentUserId: String?, names: Map<String, String>): Who {
    if (uid.isBlank()) return Who.Unknown
    if (uid == currentUserId) return Who.You
    return names[uid]?.takeIf { it.isNotBlank() }?.let(Who::Named) ?: Who.Unknown
}

/**
 * Las frases de un «cuándo»: una por tipo de tiempo, porque lo que va delante cambia con cada uno
 * —«hace 5 min», «hoy a las 18:45», «el 01 ene»—. Aparcar, reportar y resolver tienen las suyas.
 */
class TimeSentences(
    val justNow: StringResource,
    val minutesAgo: StringResource,
    val hoursAgo: StringResource,
    val todayAt: StringResource,
    val yesterdayAt: StringResource,
    val onDate: StringResource
)

val ParkedSentences = TimeSentences(
    Res.string.parked_just_now, Res.string.parked_minutes_ago, Res.string.parked_hours_ago,
    Res.string.parked_today_at, Res.string.parked_yesterday_at, Res.string.parked_on_date
)

/**
 * «Aparcado hace 5 min por Edu», «Reportada ayer a las 18:45 por ti», «Resuelta el 12 nov»: el cuándo
 * con las frases de [sentences], y el quién si lo hay. Se refresca cada minuto.
 */
@Composable
fun whenWhoText(timestamp: Long, who: Who, sentences: TimeSentences): String {
    val whenText = sentenceFor(rememberRelativeTime(timestamp), sentences)
    return when (who) {
        Who.You -> stringResource(Res.string.parked_by_you, whenText)
        is Who.Named -> stringResource(Res.string.parked_by, whenText, who.name)
        Who.Unknown -> whenText
    }
}

/**
 * «Aparcado hace 5 min por Edu», en una línea (spec 016): lo que dicen la tarjeta del mapa, la lista de
 * *Vehículos* y el detalle. Sin ubicación, «Aún no aparcado». Si quien aparcó fue quien mira, «por ti».
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
        whenWhoText(location.timestamp, whoParked(location, currentUserId), ParkedSentences)
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
private fun sentenceFor(time: RelativeTime, sentences: TimeSentences): String = when (time) {
    RelativeTime.JustNow -> stringResource(sentences.justNow)
    is RelativeTime.MinutesAgo -> stringResource(sentences.minutesAgo, time.minutes)
    is RelativeTime.HoursAgo -> stringResource(sentences.hoursAgo, time.hours)
    is RelativeTime.TodayAt -> stringResource(sentences.todayAt, time.time)
    is RelativeTime.YesterdayAt -> stringResource(sentences.yesterdayAt, time.time)
    is RelativeTime.AbsoluteDate ->
        stringResource(sentences.onDate, time.day, monthOf(time.monthKey)?.let { stringResource(it) } ?: "")
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
