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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.presentation.components.AppleSignInButton
import com.albertomedina.apark.presentation.components.GoogleSignInButton
import com.albertomedina.apark.presentation.components.StandardAparKButton
import com.albertomedina.apark.presentation.components.StandardAparKTextButton
import com.albertomedina.apark.utils.SnackbarMessage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = koinViewModel(),
    onNavigateToHome: (String) -> Unit,
    onNavigateToVerify: () -> Unit,
    onNavigateToResetPassword: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by remember { mutableStateOf(false) }
    var activeSnackbarMessage by remember { mutableStateOf<SnackbarMessage?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.shouldNavigate) {
        if (state.shouldNavigate) {
            onNavigateToHome(state.email)
            viewModel.onEvent(LoginEvent.OnNavigated)
        }
    }

    LaunchedEffect(state.shouldVerificate) {
        if (state.shouldVerificate) {
            onNavigateToVerify()
            viewModel.onEvent(LoginEvent.OnNavigated)
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

    // Traducción de las llaves de error que emite el ViewModel
    val translatedText = when (state.snackbarMessage?.message) {
        "error_invalid_email" -> stringResource(Res.string.error_invalid_email)
        "error_empty_password" -> stringResource(Res.string.error_empty_password)
        "error_verify_email" -> stringResource(Res.string.error_verify_email)
        "error_invalid_credentials" -> stringResource(Res.string.error_invalid_credentials)
        "error_google_login" -> stringResource(Res.string.error_google_login)
        "error_apple_login" -> stringResource(Res.string.error_apple_login)
        // Si no coincide con ninguna llave, asumimos que es un texto normal (ej. un error directo de Firebase)
        else -> state.snackbarMessage?.message
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            activeSnackbarMessage = msg
            snackbarHostState.showSnackbar(
                message = translatedText ?: msg.message,
                actionLabel = msg.actionLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
            viewModel.onEvent(LoginEvent.ErrorDismissed)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                activeSnackbarMessage?.let { customMsg ->
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = customMsg.backgroundColor(),
                        contentColor = customMsg.contentColor(),
                        actionColor = customMsg.contentColor()
                    )
                } ?: Snackbar(snackbarData = snackbarData)
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
                text = stringResource(Res.string.login_welcome),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = stringResource(Res.string.login_subtitle),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.onEvent(LoginEvent.EmailChanged(it)) },
                label = { Text(stringResource(Res.string.email_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                label = { Text(stringResource(Res.string.password_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) stringResource(Res.string.hide_password) else stringResource(Res.string.show_password)

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                }
            )

            StandardAparKTextButton(
                text = stringResource(Res.string.forgot_password),
                modifier = Modifier.align(Alignment.End),
                onClick = { viewModel.onEvent(LoginEvent.ResetPasswordClicked) }
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
                    Text(stringResource(Res.string.login_button),
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
                    buttonText = stringResource(Res.string.google_login),
                    onTokenReceived = { idToken, accessToken ->
                        viewModel.onEvent(LoginEvent.GoogleLoginClicked(idToken, accessToken))
                    },
                    onError = { errorMessage ->
                        println("Error en GoogleSignInButton: $errorMessage")
                    }
                )
                AppleSignInButton(
                    modifier = Modifier.fillMaxWidth(),
                    onTokenReceived = { idToken, nonce ->
                        viewModel.onEvent(LoginEvent.AppleLoginClicked(idToken, nonce))
                    },
                    onError = { errorMessage ->
                        println("Error en AppleSignInButton: $errorMessage")
                    }
                )

                StandardAparKTextButton(
                    text = stringResource(Res.string.no_account_register),
                    onClick = { viewModel.onEvent(LoginEvent.RegisterClicked) }
                )
            }
        }
    }
}
