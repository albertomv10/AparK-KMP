package com.albertomedina.apark.presentation.auth.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBar
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
import com.albertomedina.apark.presentation.components.StandardAparKButton
import com.albertomedina.apark.utils.SnackbarMessage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = koinViewModel(),
    onNavigateToVerify: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var activeSnackbarMessage by remember { mutableStateOf<SnackbarMessage?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current


    LaunchedEffect(state.shouldNavigateToVerify) {
        if (state.shouldNavigateToVerify) {
            onNavigateToVerify()
            viewModel.onEvent(RegisterEvent.OnNavigated)
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            activeSnackbarMessage = msg
            snackbarHostState.showSnackbar(
                message = msg.message,
                actionLabel = msg.actionLabel,
                withDismissAction = true,
                duration = SnackbarDuration.Long

            )
            viewModel.onEvent(RegisterEvent.ErrorDismissed)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState){ snackbarData ->
                activeSnackbarMessage?.let {
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = it.backgroundColor(),
                        contentColor = it.contentColor(),
                        actionColor = it.contentColor()
                    )
                }

            } },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.clickable { onBack() })
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 0.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(Res.string.register_title),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = stringResource(Res.string.register_subtitle),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Campo Email
            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.onEvent(RegisterEvent.EmailChanged(it)) },
                label = { Text(stringResource(Res.string.email_label)) },
                modifier = Modifier.fillMaxWidth(),
                isError = state.emailError != null,
                supportingText = { state.emailError?.let { Text(getErrorMessage(it)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo Contraseña
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(RegisterEvent.PasswordChanged(it)) },
                label = { Text(stringResource(Res.string.password_label)) },
                modifier = Modifier.fillMaxWidth(),
                isError = state.passwordError != null,
                supportingText = { state.passwordError?.let { Text(getErrorMessage(it)) } },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Campo Confirmar Contraseña
            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = { viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged(it)) },
                label = { Text(stringResource(Res.string.confirm_password_label)) },
                modifier = Modifier.fillMaxWidth(),
                isError = state.confirmPasswordError != null,
                supportingText = { state.confirmPasswordError?.let { Text(getErrorMessage(it)) } },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                StandardAparKButton(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.onEvent(RegisterEvent.RegisterClicked) }

                ) {
                    Text(stringResource(Res.string.register_button))
                }
            }
        }
    }
}

@Composable
fun getErrorMessage(key: String): String {
    return when (key) {
        "error_empty_email" -> stringResource(Res.string.error_empty_email)
        "error_invalid_email" -> stringResource(Res.string.error_invalid_email)
        "error_empty_password" -> stringResource(Res.string.error_empty_password)
        "error_password_too_short" -> stringResource(Res.string.error_password_too_short)
        "error_password_no_uppercase" -> stringResource(Res.string.error_password_no_uppercase)
        "error_password_no_number" -> stringResource(Res.string.error_password_no_number)
        "error_passwords_not_match" -> stringResource(Res.string.error_passwords_not_match)
        else -> key
    }
}
