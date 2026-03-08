package com.albertomedina.apark.presentation.auth.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.albertomedina.apark.presentation.components.AppleSignInButton
import com.albertomedina.apark.presentation.components.GoogleSignInButton
import com.albertomedina.apark.presentation.components.StandardAparKButton
import com.albertomedina.apark.presentation.components.StandardAparKTextButton
import com.albertomedina.apark.utils.SnackbarMessage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    // Inyectamos el ViewModel automáticamente gracias a Koin
    viewModel: LoginViewModel = koinViewModel(),
    // Callbacks de navegación para mantener la UI desacoplada del sistema de rutas
    onNavigateToHome: (String) -> Unit,
    onNavigateToVerify: () -> Unit,
    onNavigateToResetPassword: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    // 1. Observamos el estado (UI State)
    val state by viewModel.uiState.collectAsState()

    // 2. Estado para el Snackbar (Notificaciones)
    val snackbarHostState = remember { SnackbarHostState() }

    // 3. Estado local para mostrar/ocultar contraseña
    var passwordVisible by remember { mutableStateOf(false) }

    var activeSnackbarMessage by remember { mutableStateOf<SnackbarMessage?>(null) }

    val keyboardController = LocalSoftwareKeyboardController.current


    // ==========================================
    // MANEJO DE EFECTOS (Side Effects)
    // ==========================================

    // Escuchar si hay que navegar al Home
    LaunchedEffect(state.shouldNavigate) {
        if (state.shouldNavigate) {
            onNavigateToHome(state.email)
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

    LaunchedEffect(state.shouldResetPassword){
        if (state.shouldResetPassword){
            onNavigateToResetPassword()
            viewModel.onEvent(LoginEvent.OnNavigated)
        }
    }

    LaunchedEffect(state.shouldRegister){
        if (state.shouldRegister){
            onNavigateToRegister()
            viewModel.onEvent(LoginEvent.OnNavigated)
        }
    }

    // Escuchar si hay un mensaje de error/éxito
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            activeSnackbarMessage = msg // 👈 Guardamos el objeto para extraer sus colores luego[cite: 5]

            snackbarHostState.showSnackbar(
                message = msg.message,
                actionLabel = msg.actionLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
            viewModel.onEvent(LoginEvent.ErrorDismissed)
        }
    }

    // ==========================================
    // DIBUJO DE LA INTERFAZ (UI)
    // ==========================================

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                // Customizamos el componente visual del Snackbar
                activeSnackbarMessage?.let { customMsg ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = customMsg.backgroundColor(),
                        contentColor = customMsg.contentColor(),
                        actionColor = customMsg.contentColor() // Color del botón de acción si lo hubiera
                    )
                } ?: Snackbar(snackbarData = snackbarData) // Fallback de seguridad
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.background),
                        startY = 0f, endY = 1100f
                    )
                )
                .padding(paddingValues)
                .padding(32.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bienvenido/a",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = "Inicia sesión o crea una cuenta para empezar",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Campo de Email
            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.onEvent(LoginEvent.EmailChanged(it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            // Campo de Contraseña
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                }
            )

            StandardAparKTextButton(
                text = "Olvidé mi contraseña",
                modifier = Modifier.align(Alignment.End),
                onClick = { /*TODO*/ }
            )


            if (state.isLoading) {
                CircularProgressIndicator()
            } else {

                StandardAparKButton(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.onEvent(LoginEvent.LoginClicked)
                    }
                ){
                    Text("Iniciar Sesión",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth()
                    .height(1.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                GoogleSignInButton(
                    modifier = Modifier.fillMaxWidth(),
                    buttonText = "Continuar con Google",
                    onTokenReceived = { idToken, accessToken ->
                        viewModel.onEvent(LoginEvent.GoogleLoginClicked(idToken, accessToken))
                    },
                    onError = { errorMessage ->
                        // Manejar el error
                        println("Error en GoogleSignInButton: $errorMessage")
                    }
                )
                AppleSignInButton(
                    modifier = Modifier.fillMaxWidth(),
                    onTokenReceived = { idToken, nonce ->
                        viewModel.onEvent(LoginEvent.AppleLoginClicked(idToken, nonce))
                    },
                    onError = { errorMessage ->
                        // Manejar el error
                        println("Error en AppleSignInButton: $errorMessage")
                    }
                )

                StandardAparKTextButton(
                    text = "¿No tienes una cuenta? Regístrate",
                    onClick = { viewModel.onEvent(LoginEvent.RegisterClicked) }
                )
            }
        }
    }
}
