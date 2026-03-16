package com.albertomedina.apark.utils

import androidx.compose.runtime.Composable
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.error_empty_email
import apark.composeapp.generated.resources.error_empty_password
import apark.composeapp.generated.resources.error_invalid_email
import apark.composeapp.generated.resources.error_password_no_number
import apark.composeapp.generated.resources.error_password_no_uppercase
import apark.composeapp.generated.resources.error_password_too_short
import apark.composeapp.generated.resources.error_passwords_not_match
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
        else -> key
    }
}