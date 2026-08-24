package com.albertomedina.apark.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * Preferencias que viven **en el dispositivo**, no en Firestore.
 *
 * Aquí solo va lo que es de este móvil y de nadie más: el tema, el idioma. Las preferencias de
 * notificación serán distintas — quien decide si mandarte un push es una Cloud Function, así que
 * esas tienen que vivir en `users/{uid}` y aquí, como mucho, cachearse.
 */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
