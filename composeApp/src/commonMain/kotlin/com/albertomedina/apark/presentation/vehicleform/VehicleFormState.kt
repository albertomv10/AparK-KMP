package com.albertomedina.apark.presentation.vehicleform

import com.albertomedina.apark.domain.model.Powertrain
import com.albertomedina.apark.domain.model.VehicleDetails
import com.albertomedina.apark.domain.model.fuelFor

/**
 * El formulario de un vehículo (spec 015), el mismo al crear y al editar.
 *
 * Los ViewModel guardan uno de estos y le pasan cada evento a [reduce], que es pura: así la lógica
 * del formulario —el nombre automático, el combustible según el sistema— se prueba sin pantalla.
 */
data class VehicleFormState(
    val details: VehicleDetails = VehicleDetails(name = ""),
    val nameError: Boolean = false,
    /** «Más datos»: plegado al crear, para que el alta siga siendo rápida, y abierto al editar. */
    val showMore: Boolean = false
)

sealed interface VehicleFormEvent {
    /** Elegir el que ya estaba elegido lo quita: todos los datos son opcionales. */
    data class TypeSelected(val code: String) : VehicleFormEvent
    data class MakeChanged(val make: String) : VehicleFormEvent
    data class ModelChanged(val model: String) : VehicleFormEvent
    data class NameChanged(val name: String) : VehicleFormEvent
    data class PlateChanged(val plate: String) : VehicleFormEvent
    data class PowertrainSelected(val code: String) : VehicleFormEvent
    data class FuelSelected(val code: String) : VehicleFormEvent
    data class ColorSelected(val code: String) : VehicleFormEvent
    data class EmissionsLabelSelected(val code: String) : VehicleFormEvent
    data object MoreToggled : VehicleFormEvent
}

/** Los mismos límites que las reglas de Firestore: el campo no deja pasar de ahí. */
const val NAME_MAX_LENGTH = 60
const val PLATE_MAX_LENGTH = 15
const val MAKE_MODEL_MAX_LENGTH = 40

fun reduce(state: VehicleFormState, event: VehicleFormEvent): VehicleFormState {
    val d = state.details
    return when (event) {
        is VehicleFormEvent.TypeSelected -> state.withDetails(d.copy(type = toggle(d.type, event.code)))
        is VehicleFormEvent.MakeChanged -> {
            val make = event.make.take(MAKE_MODEL_MAX_LENGTH)
            state.withDetails(d.copy(make = make, name = autoName(d.name, d.make, d.model, make, d.model)))
                .clearNameErrorIfFilled()
        }
        is VehicleFormEvent.ModelChanged -> {
            val model = event.model.take(MAKE_MODEL_MAX_LENGTH)
            state.withDetails(d.copy(model = model, name = autoName(d.name, d.make, d.model, d.make, model)))
                .clearNameErrorIfFilled()
        }
        is VehicleFormEvent.NameChanged ->
            state.copy(details = d.copy(name = event.name.take(NAME_MAX_LENGTH)), nameError = false)
        is VehicleFormEvent.PlateChanged -> state.withDetails(d.copy(licensePlate = event.plate.take(PLATE_MAX_LENGTH)))
        is VehicleFormEvent.PowertrainSelected -> {
            val code = toggle(d.powertrain, event.code)
            state.withDetails(d.copy(powertrain = code, fuel = fuelFor(Powertrain.fromCode(code), d.fuel)))
        }
        is VehicleFormEvent.FuelSelected -> state.withDetails(d.copy(fuel = toggle(d.fuel, event.code)))
        is VehicleFormEvent.ColorSelected -> state.withDetails(d.copy(color = toggle(d.color, event.code)))
        is VehicleFormEvent.EmissionsLabelSelected ->
            state.withDetails(d.copy(emissionsLabel = toggle(d.emissionsLabel, event.code)))
        VehicleFormEvent.MoreToggled -> state.copy(showMore = !state.showMore)
    }
}

/**
 * El nombre cuando cambian la marca o el modelo. **Si está vacío, o es justo el que se generó con la
 * marca y el modelo de antes, se regenera**; si no, lo ha escrito alguien y no se toca. Sirve igual al
 * crear y al editar: un nombre puesto a mano nunca se pisa, y uno automático sigue a la marca y el
 * modelo.
 */
fun autoName(current: String, oldMake: String, oldModel: String, newMake: String, newModel: String): String =
    if (current.isBlank() || current.trim() == generatedName(oldMake, oldModel)) {
        generatedName(newMake, newModel).take(NAME_MAX_LENGTH)
    } else {
        current
    }

/** «Marca Modelo», sin espacios de sobra si falta alguno de los dos. */
fun generatedName(make: String, model: String): String =
    listOf(make.trim(), model.trim()).filter { it.isNotEmpty() }.joinToString(" ")

/**
 * Lo que se guarda: sin espacios en los bordes y la matrícula en mayúsculas. `null` si falta el
 * nombre, que es lo único obligatorio; la pantalla lo marca con [VehicleFormState.nameError].
 */
fun VehicleFormState.toSave(): VehicleDetails? {
    val name = details.name.trim()
    if (name.isEmpty()) return null
    return details.copy(
        name = name,
        licensePlate = details.licensePlate.trim().uppercase(),
        make = details.make.trim(),
        model = details.model.trim()
    )
}

/** Lo que hay que enseñar si se intenta guardar sin nombre. */
fun VehicleFormState.withNameError(): VehicleFormState = copy(nameError = true)

private fun VehicleFormState.withDetails(details: VehicleDetails) = copy(details = details)

private fun VehicleFormState.clearNameErrorIfFilled() =
    if (nameError && details.name.isNotBlank()) copy(nameError = false) else this

private fun toggle(current: String, chosen: String): String = if (current == chosen) "" else chosen
