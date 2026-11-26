package com.albertomedina.apark.domain.model

/**
 * Las categorías de las incidencias (spec 019). **Solo agrupan** las opciones del formulario: no se
 * guardan, porque cada tipo pertenece a una sola. El orden es el de los chips.
 */
enum class IncidentCategory {
    LEVELS,
    MECHANICS,
    TYRES,
    BODY;

    /** Sus tipos, en el orden en que se ofrecen. */
    val types: List<IncidentType> get() = IncidentType.entries.filter { it.category == this }
}

/**
 * Qué le pasa al vehículo (spec 019): la incidencia **es** su tipo. Como las listas de la spec 015,
 * en el documento se guarda el [code] en texto y se lee con [fromCode], así que un código que esta
 * versión no conoce no rompe nada. Las reglas de Firestore validan los mismos códigos: **añadir uno
 * es tocar el `enum` y las reglas en la misma versión**.
 *
 * [CUSTOM] es lo que no encaja en ninguno: no tiene categoría, y es el único que guarda un nombre,
 * porque los demás los traduce la app al idioma de quien mira.
 */
enum class IncidentType(val code: String, val category: IncidentCategory?) {
    LOW_FUEL("low_fuel", IncidentCategory.LEVELS),
    LOW_CHARGE("low_charge", IncidentCategory.LEVELS),
    OIL("oil", IncidentCategory.LEVELS),
    WASHER_FLUID("washer_fluid", IncidentCategory.LEVELS),
    ADBLUE("adblue", IncidentCategory.LEVELS),
    WARNING_LIGHT("warning_light", IncidentCategory.MECHANICS),
    WONT_START("wont_start", IncidentCategory.MECHANICS),
    BREAKDOWN("breakdown", IncidentCategory.MECHANICS),
    STRANGE_NOISE("strange_noise", IncidentCategory.MECHANICS),
    BULB_OUT("bulb_out", IncidentCategory.MECHANICS),
    PUNCTURE("puncture", IncidentCategory.TYRES),
    LOW_PRESSURE("low_pressure", IncidentCategory.TYRES),
    TYRE_WEAR("tyre_wear", IncidentCategory.TYRES),
    DAMAGE("damage", IncidentCategory.BODY),
    CLEANING("cleaning", IncidentCategory.BODY),
    ITEMS_INSIDE("items_inside", IncidentCategory.BODY),
    CUSTOM("custom", null);

    /**
     * Si tiene sentido para un vehículo con este sistema y este combustible (spec 015): en un
     * eléctrico no se ofrece «Poco combustible», y «AdBlue» solo en un diésel. Sin datos, todos: no
     * se esconde nada por no saber.
     */
    fun appliesTo(powertrain: Powertrain?, fuel: Fuel?): Boolean = when (this) {
        LOW_FUEL -> powertrain != Powertrain.ELECTRIC
        // Un eléctrico o uno de pila de combustible no llevan aceite de motor.
        OIL -> powertrain != Powertrain.ELECTRIC && powertrain != Powertrain.FUEL_CELL
        // Solo se carga lo que se enchufa: un híbrido normal no.
        LOW_CHARGE -> powertrain == null || powertrain == Powertrain.ELECTRIC ||
            powertrain == Powertrain.PLUG_IN_HYBRID || powertrain == Powertrain.OTHER
        ADBLUE -> if (fuel != null) {
            fuel == Fuel.DIESEL
        } else {
            powertrain != Powertrain.ELECTRIC && powertrain != Powertrain.FUEL_CELL
        }
        else -> true
    }

    companion object {
        /**
         * Un tipo desconocido —de una versión más nueva, o vacío— se enseña como una personalizada
         * sin nombre: «Incidencia». Siempre hay algo que mostrar.
         */
        fun fromCode(code: String): IncidentType = entries.firstOrNull { it.code == code } ?: CUSTOM
    }
}
