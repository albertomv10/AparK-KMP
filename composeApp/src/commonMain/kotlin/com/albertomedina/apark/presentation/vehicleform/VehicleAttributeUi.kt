package com.albertomedina.apark.presentation.vehicleform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.RvHookup
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.model.EmissionsLabel
import com.albertomedina.apark.domain.model.Fuel
import com.albertomedina.apark.domain.model.Powertrain
import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.VehicleColor
import com.albertomedina.apark.domain.model.VehicleType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Los textos de cada código. Un código que esta versión no conoce ya llega como OTHER desde
// `fromCode`, o como `null` en el color y la etiqueta, así que aquí no hay caso desconocido que
// tratar (criterio 7).

val VehicleType.label: StringResource
    get() = when (this) {
        VehicleType.CAR -> Res.string.vehicle_type_car
        VehicleType.MOTORCYCLE -> Res.string.vehicle_type_motorcycle
        VehicleType.VAN -> Res.string.vehicle_type_van
        VehicleType.MOTORHOME -> Res.string.vehicle_type_motorhome
        VehicleType.OTHER -> Res.string.vehicle_type_other
    }

val VehicleType?.icon: ImageVector
    get() = when (this) {
        VehicleType.CAR -> Icons.Filled.DirectionsCar
        VehicleType.MOTORCYCLE -> Icons.Filled.TwoWheeler
        VehicleType.VAN -> Icons.Filled.AirportShuttle
        VehicleType.MOTORHOME -> Icons.Filled.RvHookup
        VehicleType.OTHER, null -> Icons.Filled.Garage
    }

val Powertrain.label: StringResource
    get() = when (this) {
        Powertrain.COMBUSTION -> Res.string.powertrain_combustion
        Powertrain.MILD_HYBRID -> Res.string.powertrain_mild_hybrid
        Powertrain.HYBRID -> Res.string.powertrain_hybrid
        Powertrain.PLUG_IN_HYBRID -> Res.string.powertrain_plug_in_hybrid
        Powertrain.ELECTRIC -> Res.string.powertrain_electric
        Powertrain.FUEL_CELL -> Res.string.powertrain_fuel_cell
        Powertrain.OTHER -> Res.string.powertrain_other
    }

val Fuel.label: StringResource
    get() = when (this) {
        Fuel.PETROL -> Res.string.fuel_petrol
        Fuel.DIESEL -> Res.string.fuel_diesel
        Fuel.LPG -> Res.string.fuel_lpg
        Fuel.NATURAL_GAS -> Res.string.fuel_natural_gas
        Fuel.HYDROGEN -> Res.string.fuel_hydrogen
        Fuel.OTHER -> Res.string.fuel_other
    }

/** El nombre de cada color: lo que lee el lector de pantalla en la paleta (criterio 10). */
val VehicleColor.label: StringResource
    get() = when (this) {
        VehicleColor.WHITE -> Res.string.color_white
        VehicleColor.BLACK -> Res.string.color_black
        VehicleColor.GREY -> Res.string.color_grey
        VehicleColor.SILVER -> Res.string.color_silver
        VehicleColor.BLUE -> Res.string.color_blue
        VehicleColor.RED -> Res.string.color_red
        VehicleColor.GREEN -> Res.string.color_green
        VehicleColor.YELLOW -> Res.string.color_yellow
        VehicleColor.ORANGE -> Res.string.color_orange
        VehicleColor.BROWN -> Res.string.color_brown
        VehicleColor.PINK -> Res.string.color_pink
        VehicleColor.PURPLE -> Res.string.color_purple
    }

/**
 * La muestra de cada color. Son colores de carrocería y no del tema, así que no cambian en modo
 * oscuro; el borde de [VehicleAvatar] y de la paleta es lo que hace visible el blanco o el negro.
 */
val VehicleColor.swatch: Color
    get() = when (this) {
        VehicleColor.WHITE -> Color(0xFFFFFFFF)
        VehicleColor.BLACK -> Color(0xFF1C1C1E)
        VehicleColor.GREY -> Color(0xFF8E8E93)
        VehicleColor.SILVER -> Color(0xFFC9CDD2)
        VehicleColor.BLUE -> Color(0xFF1F5AA6)
        VehicleColor.RED -> Color(0xFFC62828)
        VehicleColor.GREEN -> Color(0xFF2E7D32)
        VehicleColor.YELLOW -> Color(0xFFF6C700)
        VehicleColor.ORANGE -> Color(0xFFEF6C00)
        VehicleColor.BROWN -> Color(0xFF6D4C41)
        VehicleColor.PINK -> Color(0xFFFF48B6)
        VehicleColor.PURPLE -> Color(0xFFBD00FF)
    }

val EmissionsLabel.label: StringResource
    get() = when (this) {
        EmissionsLabel.ZERO -> Res.string.emissions_zero
        EmissionsLabel.ECO -> Res.string.emissions_eco
        EmissionsLabel.C -> Res.string.emissions_c
        EmissionsLabel.B -> Res.string.emissions_b
        EmissionsLabel.NONE -> Res.string.emissions_none
    }

/** Los colores de la pegatina de la DGT, para reconocerla de un vistazo. Sin etiqueta, ninguno. */
val EmissionsLabel.badgeColor: Color?
    get() = when (this) {
        EmissionsLabel.ZERO -> Color(0xFF1565C0)
        EmissionsLabel.ECO -> Color(0xFF00897B)
        EmissionsLabel.C -> Color(0xFF2E7D32)
        EmissionsLabel.B -> Color(0xFFF6C700)
        EmissionsLabel.NONE -> null
    }

/** Negro o blanco, lo que se lea mejor encima de [background]. */
fun contentColorOn(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xDE000000) else Color.White

/**
 * Si tiene algo que enseñar el avatar: el tipo o un color que esta versión conozca. Si no, no se
 * dibuja nada (criterio 9).
 */
val Vehicle.hasAvatar: Boolean get() = type.isNotBlank() || VehicleColor.fromCode(color) != null

/**
 * El icono del tipo sobre un círculo del color del vehículo: lo que distingue un vehículo de otro de
 * un vistazo, en la tarjeta del mapa, la lista y el detalle. Sin tipo, un icono genérico; sin color,
 * solo el borde.
 */
@Composable
fun VehicleAvatar(vehicle: Vehicle, size: Dp, modifier: Modifier = Modifier) {
    val type = VehicleType.fromCode(vehicle.type)
    val color = VehicleColor.fromCode(vehicle.color)
    val fill = color?.swatch
    val description = listOfNotNull(
        type?.let { stringResource(it.label) },
        color?.let { stringResource(it.label) }
    ).joinToString(", ")

    // Sin color, hueco: un fondo gris se confundiría con un vehículo gris o plata.
    val background = if (fill != null) Modifier.background(fill, CircleShape) else Modifier
    val tint = if (fill != null) contentColorOn(fill) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Icon(type.icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.6f))
    }
}
