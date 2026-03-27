package com.albertomedina.apark.utils

import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration

/**
 * Representa los diferentes estados de tiempo relativo para ser traducidos en la UI.
 */
sealed class RelativeTime {
    data object JustNow : RelativeTime()
    data class MinutesAgo(val minutes: Int) : RelativeTime()
    data class HoursAgo(val hours: Int) : RelativeTime()
    data class YesterdayAt(val time: String) : RelativeTime()
    data class TodayAt(val time: String) : RelativeTime()
    data class AbsoluteDate(val day: String, val monthKey: String) : RelativeTime()
}

fun Long.getRelativeTime(): RelativeTime {
    val instant = Instant.fromEpochMilliseconds(this)
    val now = Clock.System.now()
    val diff = now - instant

    val seconds = diff.inWholeSeconds
    val minutes = diff.inWholeMinutes.toInt()
    val hours = diff.inWholeHours.toInt()

    val tz = TimeZone.currentSystemDefault()
    val dateTime = instant.toLocalDateTime(tz)
    val messageDate = dateTime.date
    val today = now.toLocalDateTime(tz).date

    return when {
        seconds < 60 -> RelativeTime.JustNow
        minutes < 60 -> RelativeTime.MinutesAgo(minutes)
        hours < 4 -> RelativeTime.HoursAgo(hours)
        messageDate == today -> RelativeTime.TodayAt(formatTime(dateTime))
        messageDate == today.minus(1, DateTimeUnit.DAY) -> RelativeTime.YesterdayAt(formatTime(dateTime))
        else -> RelativeTime.AbsoluteDate(
            day = dateTime.day.toString().padStart(2, '0'),
            monthKey = getMonthKey(dateTime.month)
        )
    }
}

private fun formatTime(dateTime: LocalDateTime): String {
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}

private fun getMonthKey(month: Month): String {
    return when (month) {
        Month.JANUARY -> "month_jan"; Month.FEBRUARY -> "month_feb"; Month.MARCH -> "month_mar"
        Month.APRIL -> "month_apr"; Month.MAY -> "month_may"; Month.JUNE -> "month_jun"
        Month.JULY -> "month_jul"; Month.AUGUST -> "month_aug"; Month.SEPTEMBER -> "month_sep"
        Month.OCTOBER -> "month_oct"; Month.NOVEMBER -> "month_nov"; Month.DECEMBER -> "month_dec"
        else -> ""
    }
}
