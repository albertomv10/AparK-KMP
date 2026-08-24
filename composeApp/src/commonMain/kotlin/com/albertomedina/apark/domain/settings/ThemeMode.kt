package com.albertomedina.apark.domain.settings

/**
 * Cómo se decide el tema. [SYSTEM] es el valor por defecto y el comportamiento que la app ha tenido
 * siempre: seguir al sistema operativo.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** Un valor desconocido —de una versión futura, o de un dato corrupto— cae en el defecto. */
        fun from(raw: String?): ThemeMode = entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}
