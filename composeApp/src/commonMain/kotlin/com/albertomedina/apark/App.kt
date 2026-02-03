package com.albertomedina.apark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.app_name
import apark.composeapp.generated.resources.compose_multiplatform
import com.albertomedina.apark.di.AppConfig
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@Composable
@Preview
fun App() {

    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        val viewmodel = koinViewModel<TestViewModel>()
        val textState by viewmodel.locationText.collectAsState()

        // Inyectamos la configuración para saber en pantalla si estamos en Debug
        val appConfig = koinInject<AppConfig>()
        val firestore = koinInject<FirebaseFirestore>()
        val scope = rememberCoroutineScope()
        var status by remember { mutableStateOf("Esperando...") }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = {
                showContent = !showContent
                    viewmodel.testLocation()
            }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                    Text(stringResource(Res.string.app_name))
                    Text(textState)
                }
            }

            // Chivato visual
            Text("Modo: ${if (appConfig.isDebug) "🐛 DEBUG" else "🚀 RELEASE"}")

            Text(status)

            Button(onClick = {
                scope.launch {
                    status = "Escribiendo..."
                    try {
                        // Escribimos en una colección SEGURA para no romper nada
                        val testData = mapOf(
                            "fecha" to Clock.System.now().toString(),
                            "plataforma" to getPlatform().name, // Asegúrate de tener una forma de ver la plataforma
                            "mensaje" to "Hola desde KMP!"
                        )

                        // Usamos una colección temporal
                        firestore.collection("connectivity_test").add(testData)
                        status = "✅ ¡ÉXITO! Revisa Firebase Console."
                    } catch (e: Exception) {
                        status = "❌ ERROR: ${e.message}"
                        e.printStackTrace()
                    }
                }
            }) {
                Text("PROBAR FIREBASE")
            }
        }
        
    }
}