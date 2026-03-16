package com.albertomedina.apark.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.albertomedina.apark.presentation.components.StandardAparKButton
import com.albertomedina.apark.ui.theme.AparKTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    user: String,
    viewModel: HomeViewModel = koinViewModel(),
    onNavigateToLogin: () -> Unit
) {
    val state = viewModel.uiState.collectAsState()

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column (
                Modifier.padding(paddingValues)
                    .padding(32.dp)
            ){
                Text(
                    text = "Home Screen",
                    style = MaterialTheme.typography.displayLarge
                )
                Text(
                    text = user,
                    style = MaterialTheme.typography.displaySmall
                )

                StandardAparKButton(
                    onClick = {
                        viewModel.singOut()
                        onNavigateToLogin()
                    },
                    color = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        "Cerrar sesión",
                        color = MaterialTheme.colorScheme.onError
                    )
                }

            }
        }
    }
}