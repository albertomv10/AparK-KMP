package com.albertomedina.apark.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ExtraColors(
    val success: Color,
    val onSuccess: Color
)

val LocalExtraColors = staticCompositionLocalOf {
    ExtraColors(
        success = Color.Unspecified,
        onSuccess = Color.Unspecified
    )
}

internal val extraLightColors = ExtraColors(
    success = Color(0xFF81C784),  // Verde para éxito (claro)
    onSuccess = Color.Black
)

internal val extraDarkColors = ExtraColors(
    success = Color(0xFF4CAF50), // Verde más suave (oscuro)
    onSuccess = Color.White
)

val ColorScheme.success: Color
    @Composable
    get() = LocalExtraColors.current.success

val ColorScheme.onSuccess: Color
    @Composable
    get() = LocalExtraColors.current.onSuccess
