package com.albertomedina.apark.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.albertomedina.apark.domain.settings.SettingsRepository
import com.albertomedina.apark.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = dataStore.data
        // Un fichero corrupto o ilegible no debe impedir que la app arranque: se cae al valor por
        // defecto, que es exactamente cómo se comportaba antes de que existieran las preferencias.
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { ThemeMode.from(it[THEME_MODE]) }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
