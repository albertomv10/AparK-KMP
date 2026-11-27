package com.albertomedina.apark.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * Los colores del pin: el rojo del vehículo elegido y el azul de los demás, con su borde y su punto
 * más oscuros, como el pin de Google que se usaba antes y el que sigue usando iOS.
 */
private class PinColors(val fill: Color, val dark: Color)

private val SelectedPin = PinColors(fill = Color(0xFFEA4335), dark = Color(0xFFB31412))
private val OtherPin = PinColors(fill = Color(0xFF4285F4), dark = Color(0xFF1A57B8))

private val PinWidth = 30.dp
private val PinHeight = 46.dp
private val HouseSize = 17.dp

/** La punta del pin, abajo en el centro: es lo que tiene que caer donde está el coche. */
val VehiclePinAnchor = Offset(0.5f, 1f)

/**
 * Si un punto de la pantalla, en píxeles, cae sobre el pin cuya punta está en ([tipX], [tipY]), con un
 * margen para el dedo. Es la misma regla que aplica iOS en `MapLocationController.isOnMarker`.
 */
fun isOnPin(tipX: Float, tipY: Float, x: Float, y: Float, density: Float): Boolean {
    val halfWidth = PinWidth.value / 2 * density
    val height = PinHeight.value * density
    val margin = 8 * density
    return x in (tipX - halfWidth - margin)..(tipX + halfWidth + margin) &&
        y in (tipY - height - margin)..(tipY + margin)
}

/**
 * El pin de un vehículo en Android (spec 024), dibujado aquí y no el de Google: el de Google es un
 * `BitmapDescriptor` cerrado sobre el que no se puede pintar la casa del garaje.
 *
 * Tiene su forma y su tamaño —una gota rellena, con borde y un punto en la cabeza—, para que se vea
 * como antes y como en iOS. La cabeza es **sólida**: el icono `Place` de Material, que se usó primero,
 * tiene ahí un agujero, y la casa blanca encima se perdía contra el mapa claro.
 */
@Composable
fun VehiclePin(selected: Boolean, atPlace: Boolean) {
    val colors = if (selected) SelectedPin else OtherPin
    Box(Modifier.size(PinWidth, PinHeight)) {
        Canvas(Modifier.size(PinWidth, PinHeight)) {
            val stroke = 1.5.dp.toPx()
            val radius = size.width / 2 - stroke / 2
            val center = Offset(size.width / 2, size.width / 2)
            val tip = Offset(size.width / 2, size.height - stroke / 2)

            // La gota: el círculo de la cabeza y las dos tangentes que bajan hasta la punta.
            val distance = tip.y - center.y
            val angle = acos(radius / distance)
            val startDegrees = Math.toDegrees((Math.PI / 2 + angle)).toFloat()
            val sweepDegrees = Math.toDegrees((2 * Math.PI - 2 * angle)).toFloat()
            val path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(center.x - radius * sin(angle).toFloat(), center.y + radius * cos(angle).toFloat())
                arcTo(
                    Rect(center, radius),
                    startAngleDegrees = startDegrees,
                    sweepAngleDegrees = sweepDegrees,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(path, colors.fill)
            drawPath(path, colors.dark, style = Stroke(width = stroke))

            // Sin casa, el punto de siempre en la cabeza.
            if (!atPlace) drawCircle(colors.dark, radius = radius * 0.36f, center = center)
        }
        if (atPlace) {
            Icon(
                Icons.Filled.Home,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = PinWidth / 2 - HouseSize / 2 - 0.5.dp)
                    .size(HouseSize)
            )
        }
    }
}
