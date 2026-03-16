package com.albertomedina.apark.presentation.auth.verification

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.presentation.components.StandardAparKButton
import com.albertomedina.apark.utils.SnackbarMessage
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalResourceApi::class)
@Composable
fun EmailVerificationScreen(
    viewModel: EmailVerificationViewModel = koinViewModel(),
    onNavigateToHome: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeSnackbarMessage by remember { mutableStateOf<SnackbarMessage?>(null) }

    var animationJson by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        animationJson = Res.readBytes("files/email_animation.json").decodeToString()
    }

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(animationJson)
    }

    LaunchedEffect(state.isVerificationComplete) {
        if (state.isVerificationComplete) {
            onNavigateToHome(state.email)
            viewModel.onEvent(EmailVerificationEvent.NavigationHandled)
        }
    }

    LaunchedEffect(state.shouldNavigateToLogin) {
        if (state.shouldNavigateToLogin) {
            onNavigateToLogin()
            viewModel.onEvent(EmailVerificationEvent.NavigationHandled)
        }
    }

    val currentMessage = state.snackbarMessage
    val translatedText = when (currentMessage?.message) {
        "info_not_verified_yet" -> stringResource(Res.string.info_not_verified_yet)
        "success_resend_email" -> stringResource(Res.string.success_resend_email)
        "error_creating_user_db" -> stringResource(Res.string.error_creating_user_db)
        "error_unknown" -> stringResource(Res.string.error_unknown)
        else -> currentMessage?.message
    }

    LaunchedEffect(currentMessage) {
        currentMessage?.let { msg ->
            activeSnackbarMessage = msg
            snackbarHostState.showSnackbar(
                message = translatedText ?: "",
                actionLabel = msg.actionLabel
            )
            viewModel.onEvent(EmailVerificationEvent.ErrorDismissed)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                activeSnackbarMessage?.let { customMsg ->
                    androidx.compose.material3.Snackbar(
                        snackbarData = snackbarData,
                        containerColor = customMsg.backgroundColor(),
                        contentColor = customMsg.contentColor(),
                        actionColor = customMsg.contentColor()
                    )
                } ?: androidx.compose.material3.Snackbar(snackbarData = snackbarData)
            }
        }
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
                text = stringResource(Res.string.email_verification_title),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animación Lottie
            if (animationJson.isNotEmpty()) {
                Image(
                    painter = rememberLottiePainter(
                        composition = composition,
                        iterations = Compottie.IterateForever
                    ),
                    contentDescription = "Lottie animation",
                    modifier = Modifier.size(250.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(250.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CORRECCIÓN EMAIL: stringResource con argumento
            Text(
                text = stringResource(Res.string.email_verification_description, state.email),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                StandardAparKButton(
                    onClick = { viewModel.onEvent(EmailVerificationEvent.CheckVerificationClicked) }
                ) {
                    Text(
                        stringResource(Res.string.email_verification_button_check),
                        color = MaterialTheme.colorScheme.onPrimary
                        )
                }


                StandardAparKButton(
                    onClick = { viewModel.onEvent(EmailVerificationEvent.ResendEmailClicked) },
                    color = MaterialTheme.colorScheme.inversePrimary
                ) {
                    Text(
                        stringResource(Res.string.email_verification_button_resend),
                        color = MaterialTheme.colorScheme.inverseOnSurface

                    )
                }


                StandardAparKButton(
                    onClick = { viewModel.onEvent(EmailVerificationEvent.BackToLoginClicked) },
                    color = MaterialTheme.colorScheme.inversePrimary
                ) {
                    Text(stringResource(Res.string.email_verification_button_back))
                }
            }
        }
    }
}
