package com.albertomedina.apark.presentation.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(""))
    val uiState = _uiState.asStateFlow()

}

data class HomeUiState (
    val user : String
)