package com.albertomedina.apark

/**
 * La dirección de los emuladores, de `APARK_EMULATOR_HOST` en `local.properties` (o del entorno).
 *
 * Igual que el proveedor de App Check, lo decide **el source set de la variante** y no un `if` en
 * tiempo de ejecución: `BuildConfig.EMULATOR_HOST` solo se genera para debug, de modo que esta
 * misma línea no compilaría en release. La separación no es estilo, es la garantía.
 */
fun emulatorHostOrNull(): String? = BuildConfig.EMULATOR_HOST.takeIf { it.isNotBlank() }
