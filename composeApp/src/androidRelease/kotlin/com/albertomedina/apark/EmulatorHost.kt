package com.albertomedina.apark

/**
 * En release no hay emuladores que valgan, y no es una decisión que se tome en tiempo de ejecución:
 * aquí no existe el `BuildConfig.EMULATOR_HOST` que lee la versión de debug, así que no hay nada
 * que leer ni forma de encenderlo.
 */
fun emulatorHostOrNull(): String? = null
