package com.albertomedina.apark.utils

import androidx.compose.runtime.Composable
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.error_email_in_use
import apark.composeapp.generated.resources.error_empty_email
import apark.composeapp.generated.resources.error_empty_password
import apark.composeapp.generated.resources.error_invalid_email
import apark.composeapp.generated.resources.error_name_empty
import apark.composeapp.generated.resources.error_name_too_long
import apark.composeapp.generated.resources.error_no_network
import apark.composeapp.generated.resources.error_password_no_number
import apark.composeapp.generated.resources.error_password_no_uppercase
import apark.composeapp.generated.resources.error_password_too_short
import apark.composeapp.generated.resources.error_passwords_not_match
import apark.composeapp.generated.resources.error_same_email
import apark.composeapp.generated.resources.error_same_password
import apark.composeapp.generated.resources.error_save_name
import apark.composeapp.generated.resources.error_session_expired
import apark.composeapp.generated.resources.error_too_many_attempts
import apark.composeapp.generated.resources.error_unknown
import apark.composeapp.generated.resources.error_weak_password
import apark.composeapp.generated.resources.error_wrong_current_password
import org.jetbrains.compose.resources.stringResource

private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

fun validateEmail(email: String): String? {
    if (email.isBlank()) return "error_empty_email"
    if (!email.matches(emailRegex)) return "error_invalid_email"
    return null
}

fun validatePassword(password: String): String? {
    if (password.isBlank()) return "error_empty_password"
    if (password.length < 6) return "error_password_too_short"
    if (password.none { it.isUpperCase() }) return "error_password_no_uppercase"
    if (password.none { it.isDigit() }) return "error_password_no_number"
    return null
}

fun validateConfirmPassword(password: String, confirm: String): String? {
    if (confirm.isBlank()) return "error_empty_password"
    if (password != confirm) return "error_passwords_not_match"
    return null
}

@Composable
fun getErrorMessage(key: String): String {
    return when (key) {
        "error_empty_email" -> stringResource(Res.string.error_empty_email)
        "error_invalid_email" -> stringResource(Res.string.error_invalid_email)
        "error_empty_password" -> stringResource(Res.string.error_empty_password)
        "error_password_too_short" -> stringResource(Res.string.error_password_too_short)
        "error_password_no_uppercase" -> stringResource(Res.string.error_password_no_uppercase)
        "error_password_no_number" -> stringResource(Res.string.error_password_no_number)
        "error_passwords_not_match" -> stringResource(Res.string.error_passwords_not_match)
        "error_email_in_use" -> stringResource(Res.string.error_email_in_use)
        "error_weak_password" -> stringResource(Res.string.error_weak_password)
        "error_wrong_current_password" -> stringResource(Res.string.error_wrong_current_password)
        "error_same_email" -> stringResource(Res.string.error_same_email)
        "error_same_password" -> stringResource(Res.string.error_same_password)
        "error_session_expired" -> stringResource(Res.string.error_session_expired)
        "error_name_empty" -> stringResource(Res.string.error_name_empty)
        "error_name_too_long" -> stringResource(Res.string.error_name_too_long)
        "error_save_name" -> stringResource(Res.string.error_save_name)
        "error_no_network" -> stringResource(Res.string.error_no_network)
        "error_too_many_attempts" -> stringResource(Res.string.error_too_many_attempts)
        // Antes devolvía la llave tal cual, y una llave en pantalla es un texto técnico más. Los
        // ViewModels solo emiten llaves de esta lista; esto es la red por si alguna se escapa.
        else -> stringResource(Res.string.error_unknown)
    }
}