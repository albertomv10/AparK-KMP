package com.albertomedina.apark.presentation.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.ic_google_24_webp
import com.albertomedina.apark.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun GoogleSignInButton(
    modifier: Modifier,
    buttonText: String,
    onTokenReceived: (String, String?) -> Unit,
    onError: (SocialLoginFailure) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    StandardAparKButton(
        onClick = {
            coroutineScope.launch {
                launchCredManBottomSheet(context, true, onTokenReceived, onError)
            }
        },
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer
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

private suspend fun launchCredManBottomSheet(
    context: Context,
    hasFilter: Boolean = true,
    onTokenReceived: (String, String?) -> Unit,
    onError: (SocialLoginFailure) -> Unit
) {
    try {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(hasFilter)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = CredentialManager.create(context).getCredential(
            request = request,
            context = context
        )

        val credential = result.credential
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            onTokenReceived(googleIdTokenCredential.idToken, null)
        } else {
            onError(SocialLoginFailure(SocialLoginReason.UNKNOWN, "Tipo de credencial no soportado: ${credential.type}"))
        }

    } catch (e: GetCredentialCancellationException) {
        // Cerrar el selector es una decisión del usuario, no un fallo: no se avisa de nada.
        onError(SocialLoginFailure(SocialLoginReason.CANCELLED, describe(e)))
    } catch (e: NoCredentialException) {
        if (hasFilter) {
            // El primer intento solo ofrece cuentas ya autorizadas para esta app. Que no haya
            // ninguna es lo normal la primera vez, así que se reintenta mostrándolas todas.
            launchCredManBottomSheet(context, false, onTokenReceived, onError)
        } else {
            // Sin filtro y sigue sin haber cuentas: el dispositivo no tiene ninguna de Google.
            onError(SocialLoginFailure(SocialLoginReason.NO_ACCOUNTS, describe(e)))
        }
    } catch (e: GetCredentialException) {
        onError(SocialLoginFailure(SocialLoginReason.UNKNOWN, describe(e)))
    } catch (e: Throwable) {
        // Sin esta rama, cualquier fallo que no sea de las familias anteriores se escapa de la
        // corrutina y no lo ve nadie: la hoja de credenciales se cierra y la pantalla no reacciona.
        onError(SocialLoginFailure(SocialLoginReason.UNKNOWN, describe(e)))
    }
}

/**
 * El tipo de excepción es la mitad útil del diagnóstico —el `[28444]` de una configuración de
 * OAuth incompleta llega como `GetCredentialCustomException`— y `message` a menudo viene vacío.
 */
private fun describe(e: Throwable): String =
    "${e::class.simpleName}: ${e.message ?: "sin detalle"}"