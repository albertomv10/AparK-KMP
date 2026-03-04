package com.albertomedina.apark.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.ic_google_24_webp
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun GoogleSignInButton(
    modifier: Modifier,
    buttonText: String,
    onTokenReceived: (String, String?) -> Unit,
    onError: (String) -> Unit
) {
    StandardAparKButton(
        onClick = {
            // Invocamos la función de Swift. Si no está configurada, lanzamos error.
            if (iosGoogleSignInProvider != null) {
                iosGoogleSignInProvider?.invoke(
                    { token, accessToken -> onTokenReceived(token, accessToken) },
                    { errorMsg -> onError(errorMsg) }
                )
            } else {
                onError("El proveedor de Google Sign-In no está inicializado en Swift")
            }
        },
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row (
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_google_24_webp),
                contentDescription = "Icono de Google",
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = buttonText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
            
        }
    }
}