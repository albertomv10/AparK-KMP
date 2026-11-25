package com.albertomedina.apark.domain.model

/**
 * De qué va una incidencia (spec 019): una categoría, obligatoria, y si se quiere una etiqueta más
 * concreta de esa categoría. Como las listas de la spec 015, en el documento se guarda el [code] en
 * texto y se lee con `fromCode`, así que un código que esta versión no conoce no rompe nada. Las
 * reglas de Firestore validan los mismos códigos: **añadir uno es tocar el `enum` y las reglas en la
 * misma versión**.
 */
enum class IncidentCategory(val code: String) {
    LEVELS("levels"),
    MECHANICS("mechanics"),
    TYRES("tyres"),
    BODY("body"),
    OTHER("other");

    /** Las etiquetas de esta categoría, en el orden en que se ofrecen. «Otro» no tiene. */
    val tags: List<IncidentTag> get() = IncidentTag.entries.filter { it.category == this }

    companion object {
        /** Una categoría desconocida —o vacía— se enseña como «Otro»: siempre hay algo que mostrar. */
        fun fromCode(code: String): IncidentCategory = entries.firstOrNull { it.code == code } ?: OTHER
    }
}

enum class IncidentTag(val code: String, val category: IncidentCategory) {
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
    ITEMS_INSIDE("items_inside", IncidentCategory.BODY);

    /**
     * Si tiene sentido para un vehículo con este sistema y este combustible (spec 015): en un
     * eléctrico no se ofrece «Poco combustible», y «AdBlue» solo en un diésel. Sin datos, todas: no
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
        /** Una etiqueta desconocida no se enseña: la categoría ya dice de qué va. */
        fun fromCode(code: String): IncidentTag? = entries.firstOrNull { it.code == code }
    }
}
