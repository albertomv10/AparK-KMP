package com.albertomedina.apark

import androidx.compose.runtime.Composable
import com.albertomedina.apark.presentation.navigation.BasicNavigationWrapper
import com.albertomedina.apark.ui.theme.AparKTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    AparKTheme {
        BasicNavigationWrapper()
    }
}