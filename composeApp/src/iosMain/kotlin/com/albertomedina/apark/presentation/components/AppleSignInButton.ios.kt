package com.albertomedina.apark.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.ic_apple_white
import org.jetbrains.compose.resources.painterResource

var iosAppleSignInProvider: ((onSuccess: (String, String) -> Unit, onError: (String) -> Unit) -> Unit)? = null

@Composable
actual fun AppleSignInButton(
    modifier: Modifier,
    onTokenReceived: (String, String) -> Unit,
    onError: (String) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()

    val backgroundColor = if (isDarkTheme) Color.White else Color.Black
    val textColor = if (isDarkTheme) Color.Black else Color.White

    StandardAparKButton(
        onClick = {
            if (iosAppleSignInProvider != null) {
                iosAppleSignInProvider?.invoke(
                    { idToken, nonce -> onTokenReceived(idToken, nonce) },
                    { errorMsg -> onError(errorMsg) }
                )
            } else {
                onError("El proveedor de Apple no está inicializado en iOS")
            }
        },
        modifier = modifier,
        color = backgroundColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
             Image(
                 painter = painterResource(
                     Res.drawable.ic_apple_white),
                 contentDescription = "Icono de Apple",
                 modifier = Modifier.width(16.dp),
                 colorFilter = ColorFilter.tint(textColor)
             )
             Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Continuar con Apple",
                color = textColor,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}