package com.albertomedina.apark.presentation.vehicleform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import apark.composeapp.generated.resources.*
import com.albertomedina.apark.domain.catalog.VehicleCatalog
import com.albertomedina.apark.domain.catalog.searchNames
import com.albertomedina.apark.domain.model.EmissionsLabel
import com.albertomedina.apark.domain.model.Powertrain
import com.albertomedina.apark.domain.model.VehicleColor
import com.albertomedina.apark.domain.model.VehicleType
import org.jetbrains.compose.resources.stringResource

/**
 * El formulario de un vehículo, compartido por crear y editar (spec 015). Primero el tipo, la marca y
 * el modelo, porque con ellos se rellena el nombre; después la matrícula, y lo demás plegado en «Más
 * datos».
 *
 * No se desplaza solo: lo pone dentro de su pantalla quien lo usa, con lo que haga falta alrededor.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleForm(
    state: VehicleFormState,
    catalog: VehicleCatalog,
    onEvent: (VehicleFormEvent) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val d = state.details
    val type = VehicleType.fromCode(d.type)
    val focusManager = LocalFocusManager.current
    val brands = remember(catalog, type) { catalog.brandsFor(type) }
    val models = remember(catalog, d.make, type) { catalog.modelsOf(d.make, type) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FieldLabel(stringResource(Res.string.vehicle_form_type))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VehicleType.entries.forEach { option ->
                FilterChip(
                    selected = type == option,
                    onClick = { onEvent(VehicleFormEvent.TypeSelected(option.code)) },
                    label = { Text(stringResource(option.label)) },
                    leadingIcon = { Icon(option.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    enabled = enabled
                )
            }
        }

        SuggestionField(
            value = d.make,
            onValueChange = { onEvent(VehicleFormEvent.MakeChanged(it)) },
            label = stringResource(Res.string.vehicle_form_make),
            names = brands,
            enabled = enabled,
            // Elegida la marca, lo siguiente es el modelo.
            onPicked = { focusManager.moveFocus(FocusDirection.Down) }
        )

        SuggestionField(
            value = d.model,
            onValueChange = { onEvent(VehicleFormEvent.ModelChanged(it)) },
            label = stringResource(Res.string.vehicle_form_model),
            names = models,
            enabled = enabled,
            // Elegido el modelo, el nombre ya está escrito: se cierra el teclado para que se vea.
            onPicked = { focusManager.clearFocus() }
        )

        val nameIsAutomatic = d.name.isBlank() || d.name.trim() == generatedName(d.make, d.model)
        OutlinedTextField(
            value = d.name,
            onValueChange = { onEvent(VehicleFormEvent.NameChanged(it)) },
            label = { Text(stringResource(Res.string.add_vehicle_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            isError = state.nameError,
            supportingText = {
                when {
                    state.nameError -> Text(stringResource(Res.string.add_vehicle_name_error))
                    nameIsAutomatic -> Text(stringResource(Res.string.vehicle_form_name_auto))
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
        )

        OutlinedTextField(
            value = d.licensePlate,
            onValueChange = { onEvent(VehicleFormEvent.PlateChanged(it)) },
            label = { Text(stringResource(Res.string.add_vehicle_plate_label)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done)
        )

        MoreToggle(expanded = state.showMore, onClick = { onEvent(VehicleFormEvent.MoreToggled) })

        if (state.showMore) {
            MoreDetails(state, onEvent, enabled)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreDetails(state: VehicleFormState, onEvent: (VehicleFormEvent) -> Unit, enabled: Boolean) {
    val d = state.details
    val powertrain = Powertrain.fromCode(d.powertrain)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FieldLabel(stringResource(Res.string.vehicle_form_powertrain))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Powertrain.entries.forEach { option ->
                FilterChip(
                    selected = powertrain == option,
                    onClick = { onEvent(VehicleFormEvent.PowertrainSelected(option.code)) },
                    label = { Text(stringResource(option.label)) },
                    enabled = enabled
                )
            }
        }

        // El combustible depende del sistema (criterio 3): un eléctrico no se reposta, uno de pila
        // de combustible solo lleva hidrógeno, y sin sistema elegido se ofrece todo.
        FieldLabel(stringResource(Res.string.vehicle_form_fuel))
        when (powertrain) {
            Powertrain.ELECTRIC -> Hint(stringResource(Res.string.vehicle_form_fuel_electric))
            Powertrain.FUEL_CELL -> Hint(stringResource(Res.string.vehicle_form_fuel_hydrogen))
            else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (powertrain ?: Powertrain.OTHER).allowedFuels.forEach { option ->
                    FilterChip(
                        selected = d.fuel == option.code,
                        onClick = { onEvent(VehicleFormEvent.FuelSelected(option.code)) },
                        label = { Text(stringResource(option.label)) },
                        enabled = enabled
                    )
                }
            }
        }

        FieldLabel(stringResource(Res.string.vehicle_form_color))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            VehicleColor.entries.forEach { option ->
                ColorSwatch(
                    color = option,
                    selected = d.color == option.code,
                    enabled = enabled,
                    onClick = { onEvent(VehicleFormEvent.ColorSelected(option.code)) }
                )
            }
        }

        FieldLabel(stringResource(Res.string.vehicle_form_emissions))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EmissionsLabel.entries.forEach { option ->
                FilterChip(
                    selected = d.emissionsLabel == option.code,
                    onClick = { onEvent(VehicleFormEvent.EmissionsLabelSelected(option.code)) },
                    label = { Text(stringResource(option.label)) },
                    leadingIcon = option.badgeColor?.let { badge ->
                        { Box(Modifier.size(14.dp).background(badge, CircleShape)) }
                    },
                    enabled = enabled
                )
            }
        }
    }
}

/**
 * Un campo de texto con sugerencias **debajo, en línea**, y no en un menú emergente: así se comporta
 * igual en las dos plataformas y con el teclado abierto (diseño de la spec 015). Lo escrito vale
 * siempre, esté o no en la lista (criterio 5): las sugerencias solo ayudan.
 */
@Composable
private fun SuggestionField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    names: List<String>,
    enabled: Boolean,
    onPicked: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    // Si ya pone exactamente uno de la lista, porque se ha elegido o se ha escrito igual, no hay nada
    // más que sugerir. «skoda» sí sigue sugiriendo «Škoda», por si se quiere bien escrito.
    val suggestions = remember(value, names) {
        if (names.any { it == value.trim() }) emptyList() else searchNames(value, names)
    }

    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
        )
        if (focused && suggestions.isNotEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Column {
                    suggestions.forEachIndexed { index, suggestion ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) {
                                    onValueChange(suggestion)
                                    onPicked()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Una muestra de la paleta: el lector de pantalla dice su nombre y si está elegida (criterio 10). */
@Composable
private fun ColorSwatch(color: VehicleColor, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val name = stringResource(color.label)
    val fill = color.swatch
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center
    ) {
        val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        Box(
            Modifier
                .size(36.dp)
                .background(fill, CircleShape)
                .border(if (selected) 3.dp else 1.dp, ring, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = contentColorOn(fill),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MoreToggle(expanded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(Res.string.vehicle_form_more),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
