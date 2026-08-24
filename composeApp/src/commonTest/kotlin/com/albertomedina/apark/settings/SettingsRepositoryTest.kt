package com.albertomedina.apark.settings

import com.albertomedina.apark.data.settings.DataStoreSettingsRepository
import com.albertomedina.apark.data.settings.SETTINGS_FILE
import com.albertomedina.apark.data.settings.createSettingsDataStore
import com.albertomedina.apark.domain.settings.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Se ejecuta contra un directorio temporal real, no contra un doble: lo que se quiere comprobar es
 * justamente que **persiste en disco**, y un doble en memoria demostraría lo contrario de lo que
 * interesa.
 */
class SettingsRepositoryTest {

    // Un directorio por instancia: DataStore lanza si se abren dos sobre el mismo fichero, y los
    // tests de distintas plataformas pueden correr a la vez.
    private val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "apark-test-${Random.nextLong()}"

    init {
        FileSystem.SYSTEM.createDirectories(directory)
    }

    @AfterTest
    fun cleanUp() {
        FileSystem.SYSTEM.deleteRecursively(directory, mustExist = false)
    }

    private fun repository() = DataStoreSettingsRepository(createSettingsDataStore(directory.toString()))

    @Test
    fun `sin nada guardado usa SYSTEM, que es como se comportaba antes`() = runTest {
        assertEquals(ThemeMode.SYSTEM, repository().themeMode.first())
    }

    @Test
    fun `lo guardado se puede volver a leer`() = runTest {
        val repository = repository()
        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }

    @Test
    fun `lo guardado llega al disco, no se queda en memoria`() = runTest {
        repository().setThemeMode(ThemeMode.LIGHT)

        // No se comprueba abriendo una segunda instancia: DataStore exige **una por fichero y
        // proceso**, cada una con su caché, así que dos instancias no simulan un reinicio — sólo
        // demuestran que se ha incumplido el contrato. Lo que sí prueba la persistencia es que el
        // fichero exista y lleve el valor dentro; que la lectura viene del disco lo demuestra el
        // test del fichero corrupto.
        val file = directory / SETTINGS_FILE
        assertTrue(FileSystem.SYSTEM.exists(file), "no se escribió el fichero de preferencias")

        val written = FileSystem.SYSTEM.read(file) { readUtf8() }
        assertTrue(written.contains("theme_mode"), "falta la clave en el fichero")
        assertTrue(written.contains("LIGHT"), "falta el valor en el fichero")
    }

    @Test
    fun `un fichero corrupto no impide arrancar`() = runTest {
        val file = directory / SETTINGS_FILE
        FileSystem.SYSTEM.write(file) { writeUtf8("esto no es un preferences_pb") }
        assertEquals(ThemeMode.SYSTEM, repository().themeMode.first())
    }

    @Test
    fun `un valor desconocido cae en el defecto en vez de reventar`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.from("TEMA_DE_UNA_VERSION_FUTURA"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.from(null))
        assertEquals(ThemeMode.DARK, ThemeMode.from("DARK"))
    }
}
