package com.albertomedina.apark.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.FileSystem
import okio.Path.Companion.toPath

/** Nombre del fichero. DataStore exige la extensión `.preferences_pb`. */
internal const val SETTINGS_FILE = "apark.preferences_pb"

/**
 * Dónde puede escribir la app es lo único que cambia entre plataformas, así que es lo único que se
 * declara `expect`. El resto de la construcción es compartida.
 */
expect fun settingsDirectory(): String

fun createSettingsDataStore(directory: String = settingsDirectory()): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = androidx.datastore.preferences.core.PreferencesSerializer,
            producePath = { "$directory/$SETTINGS_FILE".toPath() }
        )
    )
