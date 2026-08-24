package com.albertomedina.apark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.albertomedina.apark.domain.settings.SettingsRepository
import com.albertomedina.apark.domain.settings.ThemeMode
import com.albertomedina.apark.presentation.navigation.BasicNavigationWrapper
import com.albertomedina.apark.ui.theme.AparKTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    val settings: SettingsRepository = koinInject()
    // Arranca en SYSTEM y no en "lo último leído": el primer valor del disco llega un instante
    // después, y partir del defecto evita un parpadeo de tema al abrir la app.
    val themeMode by settings.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)

    AparKTheme(themeMode = themeMode) {
        BasicNavigationWrapper()
    }
}
