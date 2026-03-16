package com.albertomedina.apark.presentation.auth.resetPassword

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.presentation.components.StandardAparKButton
import com.albertomedina.apark.utils.SnackbarMessage
import com.albertomedina.apark.utils.getErrorMessage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    viewModel: ResetPassWordViewmodel = koinViewModel(),
    onBack:() -> Unit
){
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember{ SnackbarHostState() }
    var activeSnackbarMessage by remember { mutableStateOf<SnackbarMessage?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val translatedText = when (state.snackbarMessage?.message) {
        "success_reset_password_sent" -> stringResource(Res.string.success_reset_password_sent)
        "error_invalid_email" -> stringResource(Res.string.error_invalid_email)
        "error_unknown" -> stringResource(Res.string.error_unknown)
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
            viewModel.onEvent(ResetPasswordEvent.ErrorDismissed)
        }
    }

    Scaffold (
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
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.reset_password_title)) },
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
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = if (!state.emailSent) stringResource(Res.string.reset_password_description) else stringResource(Res.string.reset_password_description_success),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.onEvent(ResetPasswordEvent.EmailChanged(email = it)) },
                label = { Text(stringResource(Res.string.email_label)) },
                modifier = Modifier.fillMaxWidth(),
                isError = state.emailError != null,
                supportingText = { state.emailError?.let { Text(getErrorMessage(it)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                StandardAparKButton(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.onEvent(ResetPasswordEvent.ResetPasswordClicked) }

                ) {
                    Text(if (!state.emailSent) stringResource(Res.string.reset_password_send_button) else stringResource(Res.string.reset_password_resend_button))
                }
            }
        }
    }
}
