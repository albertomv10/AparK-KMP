package com.albertomedina.apark.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * El mapa para elegir un punto moviéndolo bajo un marcador fijo (spec 024): el de la ubicación
 * predeterminada. El marcador no es parte del mapa —lo dibuja Compose encima, en el centro—, así que
 * aquí solo hay un mapa que avisa de dónde queda su centro cada vez que la cámara se para.
 *
 * Nace en [initial] con [initialZoom] y después lo mueve quien mira. No comparte nada con
 * [AparKMap]: en iOS, aquel es un mapa único atado al carrusel.
 */
@Composable
expect fun PlacePickerMap(
    modifier: Modifier = Modifier,
    initial: GeoPoint,
    initialZoom: Float,
    onCenterChanged: (GeoPoint) -> Unit
)
