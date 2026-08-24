package com.albertomedina.apark.data.settings

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * El directorio de documentos del contenedor de la app: es lo que iOS respalda y conserva entre
 * actualizaciones, que es justo lo que se espera de unas preferencias.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun settingsDirectory(): String {
    val url: NSURL = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    ) ?: error("iOS no devolvió el directorio de documentos")
    return requireNotNull(url.path) { "El directorio de documentos no tiene ruta" }
}
