package com.albertomedina.apark.presentation.addvehicle

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.Res
import apark.composeapp.generated.resources.add_vehicle_error_generic
import apark.composeapp.generated.resources.add_vehicle_error_not_authenticated
import apark.composeapp.generated.resources.add_vehicle_name_error
import apark.composeapp.generated.resources.add_vehicle_name_label
import apark.composeapp.generated.resources.add_vehicle_plate_label
import apark.composeapp.generated.resources.add_vehicle_save
import apark.composeapp.generated.resources.add_vehicle_back
import apark.composeapp.generated.resources.add_vehicle_tab_create
import apark.composeapp.generated.resources.add_vehicle_tab_join
import apark.composeapp.generated.resources.join_vehicle_action
import apark.composeapp.generated.resources.join_vehicle_code_error
import apark.composeapp.generated.resources.join_vehicle_code_label
import apark.composeapp.generated.resources.join_vehicle_error
import apark.composeapp.generated.resources.join_vehicle_error_already_member
import apark.composeapp.generated.resources.join_vehicle_error_expired
import apark.composeapp.generated.resources.join_vehicle_error_invalid
import apark.composeapp.generated.resources.join_vehicle_error_used
import apark.composeapp.generated.resources.add_vehicle_title
import com.albertomedina.apark.presentation.components.StandardAparKButton
import com.albertomedina.apark.utils.SnackbarMessage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleScreen(
    viewModel: AddVehicleViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeSnackbarMessage by remember { mutableStateOf<SnackbarMessage?>(null) }
    val keyboardController = LocalSoftwareKeyboardController.current

    val notAuthenticatedMessage = stringResource(Res.string.add_vehicle_error_not_authenticated)
    val genericErrorMessage = stringResource(Res.string.add_vehicle_error_generic)
    val joinInvalidMessage = stringResource(Res.string.join_vehicle_error_invalid)
    val joinUsedMessage = stringResource(Res.string.join_vehicle_error_used)
    val joinExpiredMessage = stringResource(Res.string.join_vehicle_error_expired)
    val joinAlreadyMemberMessage = stringResource(Res.string.join_vehicle_error_already_member)
    val joinGenericMessage = stringResource(Res.string.join_vehicle_error)

    // One clean form per visit. The flag is saveable, so it rides out a rotation (the entry keeps
    // its saved state) but is dropped when the entry is popped — which is exactly when the next
    // visit should start empty.
    var formCleared by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!formCleared) {
            viewModel.onEvent(AddVehicleEvent.ScreenOpened)
            formCleared = true
        }
    }

    LaunchedEffect(state.shouldNavigateBack) {
        if (state.shouldNavigateBack) {
            onBack()
            viewModel.onEvent(AddVehicleEvent.NavigationHandled)
        }
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            activeSnackbarMessage = msg
            val text = when (msg.message) {
                AddVehicleViewModel.ERROR_NOT_AUTHENTICATED_KEY -> notAuthenticatedMessage
                AddVehicleViewModel.ERROR_GENERIC_KEY -> genericErrorMessage
                AddVehicleViewModel.ERROR_JOIN_INVALID_KEY -> joinInvalidMessage
                AddVehicleViewModel.ERROR_JOIN_USED_KEY -> joinUsedMessage
                AddVehicleViewModel.ERROR_JOIN_EXPIRED_KEY -> joinExpiredMessage
                AddVehicleViewModel.ERROR_JOIN_ALREADY_MEMBER_KEY -> joinAlreadyMemberMessage
                AddVehicleViewModel.ERROR_JOIN_KEY -> joinGenericMessage
                else -> msg.message
            }
            snackbarHostState.showSnackbar(
                message = text,
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
            viewModel.onEvent(AddVehicleEvent.SnackBarDismissed)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { snackbarData ->
                activeSnackbarMessage?.let {
                    Snackbar(
                        snackbarData = snackbarData,
                        containerColor = it.backgroundColor(),
                        contentColor = it.contentColor(),
                        actionColor = it.contentColor()
                    )
                } ?: Snackbar(snackbarData = snackbarData)
            }
        },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.add_vehicle_back)
                        )
                    }
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
                text = stringResource(Res.string.add_vehicle_title),
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Both ways of gaining a vehicle live together: create your own, or join one
            // somebody shared with you.
            TabRow(selectedTabIndex = state.selectedTab, modifier = Modifier.fillMaxWidth()) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.onEvent(AddVehicleEvent.TabSelected(0)) },
                    text = { Text(stringResource(Res.string.add_vehicle_tab_create)) }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.onEvent(AddVehicleEvent.TabSelected(1)) },
                    text = { Text(stringResource(Res.string.add_vehicle_tab_join)) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (state.selectedTab == 1) {
                OutlinedTextField(
                    value = state.code,
                    onValueChange = { viewModel.onEvent(AddVehicleEvent.CodeChanged(it)) },
                    label = { Text(stringResource(Res.string.join_vehicle_code_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = state.codeError,
                    supportingText = {
                        if (state.codeError) Text(stringResource(Res.string.join_vehicle_code_error))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    StandardAparKButton(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.onEvent(AddVehicleEvent.JoinClicked)
                        }
                    ) {
                        Text(stringResource(Res.string.join_vehicle_action))
                    }
                }

                return@Column
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(AddVehicleEvent.NameChanged(it)) },
                label = { Text(stringResource(Res.string.add_vehicle_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                isError = state.nameError,
                supportingText = {
                    if (state.nameError) Text(stringResource(Res.string.add_vehicle_name_error))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.licensePlate,
                onValueChange = { viewModel.onEvent(AddVehicleEvent.LicensePlateChanged(it)) },
                label = { Text(stringResource(Res.string.add_vehicle_plate_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                StandardAparKButton(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.onEvent(AddVehicleEvent.SaveClicked)
                    }
                ) {
                    Text(stringResource(Res.string.add_vehicle_save))
                }
            }
        }
    }
}
