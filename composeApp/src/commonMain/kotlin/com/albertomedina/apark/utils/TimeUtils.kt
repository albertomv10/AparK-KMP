package com.albertomedina.apark.utils

import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Instant

// 🔹 Helpers para el formateo manual KMP (evita dependencias pesadas)
private fun Instant.formatTime(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val dateTime = this.toLocalDateTime(timeZone)
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}

private fun Instant.formatDateSpanish(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val dateTime = this.toLocalDateTime(timeZone)
    val day = dateTime.day.toString().padStart(2, '0')
    val monthStr = when (dateTime.month) {
        Month.JANUARY -> "ene"; Month.FEBRUARY -> "feb"; Month.MARCH -> "mar"
        Month.APRIL -> "abr"; Month.MAY -> "may"; Month.JUNE -> "jun"
        Month.JULY -> "jul"; Month.AUGUST -> "ago"; Month.SEPTEMBER -> "sep"
        Month.OCTOBER -> "oct"; Month.NOVEMBER -> "nov"; Month.DECEMBER -> "dic"
        else -> ""
    }
    return "$day $monthStr"
}

// 🔹 Ej: 14:35 o 25 abr
fun Long.toFormattedTime(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val tz = TimeZone.currentSystemDefault()
    val messageDate = instant.toLocalDateTime(tz).date
    val today = Clock.System.now().toLocalDateTime(tz).date

    return if (messageDate == today) instant.formatTime(tz) else instant.formatDateSpanish(tz)
}

// 🔹 Ej: Hace 5 min / Hace 2 h / Hoy 14:32 / Ayer / 25 abr
fun Long.toSmartTimeLabel(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val now = Clock.System.now()
    val diff = now - instant

    val seconds = diff.inWholeSeconds
    val minutes = diff.inWholeMinutes
    val hours = diff.inWholeHours

    val tz = TimeZone.currentSystemDefault()
    val messageDate = instant.toLocalDateTime(tz).date
    val today = now.toLocalDateTime(tz).date

    return when {
        seconds < 60 -> "Menos de un minuto"
        minutes < 60 -> "Hace $minutes min"
        hours < 4 -> "Hace $hours h"
        messageDate == today -> instant.formatTime(tz)
        messageDate == today.minus(1, DateTimeUnit.DAY) -> "Ayer"
        else -> instant.formatDateSpanish(tz)
    }
}

fun Long.toSmartTimeLabelDebugging(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val now = Clock.System.now()
    val diff = now - instant

    val seconds = diff.inWholeSeconds
    val minutes = diff.inWholeMinutes
    val hours = diff.inWholeHours

    val tz = TimeZone.currentSystemDefault()
    val messageDate = instant.toLocalDateTime(tz).date
    val today = now.toLocalDateTime(tz).date

    return when {
        seconds < 60 -> "Hace $seconds seg"
        minutes < 60 -> "Hace $minutes min"
        hours < 4 -> "Hace $hours h"
        messageDate == today -> instant.formatTime(tz)
        messageDate == today.minus(1, DateTimeUnit.DAY) -> "Ayer"
        else -> instant.formatDateSpanish(tz)
    }
}