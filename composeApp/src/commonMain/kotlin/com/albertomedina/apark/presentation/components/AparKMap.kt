package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.albertomedina.apark.domain.model.Vehicle

@Composable
expect fun AparKMap (
    modifier:Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
    vehicles: List<Vehicle>,
    selectedVehicleIndex: Int
    )
