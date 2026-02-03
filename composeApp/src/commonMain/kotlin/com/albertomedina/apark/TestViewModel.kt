package com.albertomedina.apark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.albertomedina.apark.domain.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TestViewModel(
    private val repository: LocationRepository
) : ViewModel() {

    // Estado simple: Un String para pintar en pantalla
    private val _locationText = MutableStateFlow("Pulsa el botón...")
    val locationText = _locationText.asStateFlow()

    fun testLocation() {
        viewModelScope.launch {
            _locationText.value = "Buscando satélites... 🛰️"

            repository.getUserLocation().collect { location ->
                if (location != null) {
                    _locationText.value = "📍 ÉXITO:\nLat: ${location.latitude}\nLon: ${location.longitude}"
                } else {
                    _locationText.value = "❌ Error: No se pudo obtener ubicación"
                }
            }
        }
    }
}