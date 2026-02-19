package com.albertomedina.apark.presentation.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel // 👈 Importante para inyectar el ViewModel en KMP

@Composable
fun LoginScreen(
    // Inyectamos el ViewModel automáticamente gracias a Koin
    viewModel: LoginViewModel = koinViewModel(),
    // Callbacks de navegación para mantener la UI desacoplada del sistema de rutas
    onNavigateToHome: () -> Unit,
    onNavigateToVerify: () -> Unit
) {
    // 1. Observamos el estado (UI State)
    val state by viewModel.uiState.collectAsState()

    // 2. Estado para el Snackbar (Notificaciones)
    val snackbarHostState = remember { SnackbarHostState() }

    // ==========================================
    // MANEJO DE EFECTOS (Side Effects)
    // ==========================================

    // Escuchar si hay que navegar al Home
    LaunchedEffect(state.shouldNavigate) {
        if (state.shouldNavigate) {
            onNavigateToHome()
            viewModel.onEvent(LoginEvent.OnNavigated) // Reseteamos el "gatillo"
        }
    }

    // Escuchar si hay que ir a Verificar Email
    LaunchedEffect(state.shouldVerificate) {
        if (state.shouldVerificate) {
            onNavigateToVerify()
            viewModel.onEvent(LoginEvent.OnNavigated) // Reseteamos el "gatillo"
        }
    }

    // Escuchar si hay un mensaje de error/éxito
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(LoginEvent.ErrorDismissed) // Limpiamos el error tras mostrarlo
        }
    }

    // ==========================================
    // DIBUJO DE LA INTERFAZ (UI)
    // ==========================================

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Apark",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Campo de Email
            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.onEvent(LoginEvent.EmailChanged(it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de Contraseña
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation() // Oculta los caracteres
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón o Cargando
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.onEvent(LoginEvent.LoginClicked) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Iniciar Sesión")
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        // Nota: En un entorno real, aquí llamarías a una librería
                        // que abra el popup de Google, recoja el token y se lo pase al evento.
                        // Para probar el flujo de error, pasamos un token falso.
                        viewModel.onEvent(LoginEvent.GoogleLoginClicked("token_falso_de_prueba"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Entrar con Google (Prueba)")
                }
            }
        }
    }
}