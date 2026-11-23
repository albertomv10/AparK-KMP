package com.albertomedina.apark.utils

import androidx.compose.ui.text.intl.Locale

/** Los idiomas con traducción propia en `composeResources/`. */
private val TRANSLATED_LANGUAGES = setOf("es", "en", "fr")

/** El de `values/`, sin sufijo: lo que ve quien tiene el dispositivo en cualquier otro idioma. */
private const val DEFAULT_LANGUAGE = "es"

/**
 * El idioma en que se está mostrando la app ahora mismo.
 *
 * Sale de [Locale.current], que es lo mismo que consultan los recursos de Compose para elegir los
 * textos, así que lo que esto devuelve coincide con lo que la persona está leyendo. Un idioma sin
 * traducción cae en el por defecto, igual que caen los textos.
 *
 * Se lee en el momento de usarlo, nunca se guarda: el idioma puede cambiar con la app abierta.
 */
fun appLanguage(): String =
    Locale.current.language.lowercase().takeIf { it in TRANSLATED_LANGUAGES } ?: DEFAULT_LANGUAGE
