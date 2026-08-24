package com.albertomedina.apark.data.settings

import android.content.Context

/**
 * En Android hace falta el `Context`, que Koin ya tiene registrado. Se guarda al arrancar en vez de
 * pasarlo por la firma `expect`, para que el código compartido no tenga que conocer un tipo que
 * solo existe en una plataforma.
 */
lateinit var androidSettingsContext: Context

actual fun settingsDirectory(): String = androidSettingsContext.filesDir.absolutePath
