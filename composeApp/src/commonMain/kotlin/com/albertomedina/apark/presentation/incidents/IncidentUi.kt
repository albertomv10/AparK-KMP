package com.albertomedina.apark.presentation.incidents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CarCrash
import androidx.compose.material.icons.filled.CarRepair
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.model.Incident
import com.albertomedina.apark.domain.model.IncidentCategory
import com.albertomedina.apark.domain.model.IncidentType
import com.albertomedina.apark.presentation.components.TimeSentences
import com.albertomedina.apark.ui.theme.LocalIsDarkTheme
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Los nombres y los iconos de cada código (spec 019). El de cada tipo es el que llevan el detalle,
// «Pendiente» y la tarjeta del mapa (spec 020); el de las categorías solo sale en los chips del
// formulario.

val IncidentCategory.label: StringResource
    get() = when (this) {
        IncidentCategory.LEVELS -> Res.string.incident_category_levels
        IncidentCategory.MECHANICS -> Res.string.incident_category_mechanics
        IncidentCategory.TYRES -> Res.string.incident_category_tyres
        IncidentCategory.BODY -> Res.string.incident_category_body
    }

val IncidentCategory.icon: ImageVector
    get() = when (this) {
        IncidentCategory.LEVELS -> Icons.Filled.Speed
        IncidentCategory.MECHANICS -> Icons.Filled.Build
        IncidentCategory.TYRES -> Icons.Filled.TireRepair
        IncidentCategory.BODY -> Icons.Filled.CarCrash
    }

val IncidentType.label: StringResource
    get() = when (this) {
        IncidentType.LOW_FUEL -> Res.string.incident_type_low_fuel
        IncidentType.LOW_CHARGE -> Res.string.incident_type_low_charge
        IncidentType.OIL -> Res.string.incident_type_oil
        IncidentType.WASHER_FLUID -> Res.string.incident_type_washer_fluid
        IncidentType.ADBLUE -> Res.string.incident_type_adblue
        IncidentType.WARNING_LIGHT -> Res.string.incident_type_warning_light
        IncidentType.WONT_START -> Res.string.incident_type_wont_start
        IncidentType.BREAKDOWN -> Res.string.incident_type_breakdown
        IncidentType.STRANGE_NOISE -> Res.string.incident_type_strange_noise
        IncidentType.BULB_OUT -> Res.string.incident_type_bulb_out
        IncidentType.PUNCTURE -> Res.string.incident_type_puncture
        IncidentType.LOW_PRESSURE -> Res.string.incident_type_low_pressure
        IncidentType.TYRE_WEAR -> Res.string.incident_type_tyre_wear
        IncidentType.DAMAGE -> Res.string.incident_type_damage
        IncidentType.CLEANING -> Res.string.incident_type_cleaning
        IncidentType.ITEMS_INSIDE -> Res.string.incident_type_items_inside
        IncidentType.CUSTOM -> Res.string.incident_type_custom
    }

val IncidentType.icon: ImageVector
    get() = when (this) {
        IncidentType.LOW_FUEL -> Icons.Filled.LocalGasStation
        IncidentType.LOW_CHARGE -> Icons.Filled.EvStation
        IncidentType.OIL -> Icons.Filled.OilBarrel
        IncidentType.WASHER_FLUID -> Icons.Filled.WaterDrop
        IncidentType.ADBLUE -> Icons.Filled.Science
        IncidentType.WARNING_LIGHT -> Icons.Filled.WarningAmber
        IncidentType.WONT_START -> Icons.Filled.BatteryAlert
        IncidentType.BREAKDOWN -> Icons.Filled.CarRepair
        IncidentType.STRANGE_NOISE -> Icons.Filled.Hearing
        IncidentType.BULB_OUT -> Icons.Filled.Lightbulb
        IncidentType.PUNCTURE -> Icons.Filled.TireRepair
        IncidentType.LOW_PRESSURE -> Icons.Filled.Air
        IncidentType.TYRE_WEAR -> Icons.Filled.TireRepair
        IncidentType.DAMAGE -> Icons.Filled.CarCrash
        IncidentType.CLEANING -> Icons.Filled.CleaningServices
        IncidentType.ITEMS_INSIDE -> Icons.Filled.Inventory2
        // El octógono con exclamación, un aviso genérico. El triángulo ya es «Testigo encendido».
        IncidentType.CUSTOM -> Icons.Filled.Report
    }

/**
 * El color de cada categoría, en claro y en oscuro. Sigue el tema de la app (`LocalIsDarkTheme`), no
 * el del sistema: la app tiene su propio selector en *Cuenta*. Los oscuros son los tonos 300 de la
 * misma familia, para que se lean igual sobre fondo oscuro.
 */
private data class CategoryColor(val light: Color, val dark: Color)

private val IncidentCategory.palette: CategoryColor
    get() = when (this) {
        IncidentCategory.LEVELS -> CategoryColor(Color(0xFFEF6C00), Color(0xFFFFB74D))    // naranja, como un indicador
        IncidentCategory.MECHANICS -> CategoryColor(Color(0xFFC62828), Color(0xFFEF9A9A)) // rojo, lo más urgente
        IncidentCategory.TYRES -> CategoryColor(Color(0xFF7B1FA2), Color(0xFFCE93D8))     // morado
        IncidentCategory.BODY -> CategoryColor(Color(0xFF00796B), Color(0xFF80CBC4))      // verde azulado
    }

/** Gris azulado para «Personalizada», que no es de ninguna categoría. */
private val CustomPalette = CategoryColor(Color(0xFF546E7A), Color(0xFFB0BEC5))

/** El color de un tipo: el de su categoría, o el gris de «Personalizada». */
val IncidentType.color: Color
    @Composable get() {
        val palette = category?.palette ?: CustomPalette
        return if (LocalIsDarkTheme.current) palette.dark else palette.light
    }

val IncidentCategory.color: Color
    @Composable get() = if (LocalIsDarkTheme.current) palette.dark else palette.light

/** El icono de una incidencia: el de su tipo. */
val Incident.icon: ImageVector
    get() = kind.icon

/**
 * El nombre de una incidencia: el de su tipo, traducido; o el que le puso quien la reportó, si es
 * personalizada. Una personalizada sin nombre —en la práctica, un tipo que esta versión no conoce—
 * es «Incidencia».
 */
@Composable
fun Incident.displayName(): String =
    if (kind == IncidentType.CUSTOM) title.ifBlank { stringResource(Res.string.incident_untitled) }
    else stringResource(kind.label)

/**
 * El icono de un tipo en un círculo del color de su categoría, al 16 %: se distinguen de un vistazo
 * (specs 019 y 020). Es el mismo en el detalle, en «Pendiente» y, más pequeño, en la tarjeta del mapa.
 *
 * @param color el de su categoría, salvo que quien lo pinta quiera otro: las resueltas van en gris.
 */
@Composable
fun IncidentTypeIcon(
    type: IncidentType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    color: Color = type.color
) {
    Box(
        modifier.size(size).background(color.copy(alpha = 0.16f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Decorativo: el nombre va al lado, o en la descripción de quien lo agrupa.
        Icon(type.icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
    }
}

val ReportedSentences = TimeSentences(
    Res.string.incident_reported_just_now, Res.string.incident_reported_minutes_ago,
    Res.string.incident_reported_hours_ago, Res.string.incident_reported_today_at,
    Res.string.incident_reported_yesterday_at, Res.string.incident_reported_on_date
)

val ResolvedSentences = TimeSentences(
    Res.string.incident_resolved_just_now, Res.string.incident_resolved_minutes_ago,
    Res.string.incident_resolved_hours_ago, Res.string.incident_resolved_today_at,
    Res.string.incident_resolved_yesterday_at, Res.string.incident_resolved_on_date
)

/** Solo el cuándo, sin «Reportada»: «hace 2 h», «ayer a las 18:45». Para «Pendiente» (spec 020). */
val TimeAgoSentences = TimeSentences(
    Res.string.time_just_now, Res.string.time_minutes_ago, Res.string.time_hours_ago,
    Res.string.time_today_at, Res.string.time_yesterday_at, Res.string.time_on_date
)
