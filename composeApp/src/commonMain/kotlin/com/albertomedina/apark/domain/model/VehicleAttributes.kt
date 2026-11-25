package com.albertomedina.apark.domain.model

/**
 * Las listas cerradas de los datos de un vehículo (spec 015; catálogo en `docs/DATA-MODEL.md` §9).
 *
 * En el documento se guarda el [code] como `String`, **nunca el `enum`**: deserializar directamente a
 * un `enum` lanza con un valor que no conoce, y una versión futura que añada un código rompería la app
 * de quien no ha actualizado. Por eso cada lista se lee con `fromCode`, que devuelve `null` para `""`
 * («sin dato») y `OTHER` para lo desconocido; el color y la etiqueta, que no tienen «otro», `null`
 * también para lo desconocido: no se muestran. Las reglas de Firestore validan los mismos códigos.
 */
enum class VehicleType(val code: String) {
    CAR("car"), MOTORCYCLE("motorcycle"), VAN("van"), MOTORHOME("motorhome"), OTHER("other");

    companion object {
        fun fromCode(code: String): VehicleType? = decode(code, entries, OTHER) { it.code }
    }
}

/** El sistema: qué mueve el vehículo. */
enum class Powertrain(val code: String) {
    COMBUSTION("combustion"),
    MILD_HYBRID("mild_hybrid"),
    HYBRID("hybrid"),
    PLUG_IN_HYBRID("plug_in_hybrid"),
    ELECTRIC("electric"),
    FUEL_CELL("fuel_cell"),
    OTHER("other");

    /** Lo que tiene sentido echarle. Vacío: no se le echa nada, se carga. */
    val allowedFuels: List<Fuel>
        get() = when (this) {
            COMBUSTION -> listOf(Fuel.PETROL, Fuel.DIESEL, Fuel.LPG, Fuel.NATURAL_GAS, Fuel.OTHER)
            MILD_HYBRID, HYBRID, PLUG_IN_HYBRID -> listOf(Fuel.PETROL, Fuel.DIESEL)
            ELECTRIC -> emptyList()
            FUEL_CELL -> listOf(Fuel.HYDROGEN)
            OTHER -> Fuel.entries
        }

    /** Si hay que preguntar qué le echas. Un eléctrico no se reposta; uno de pila, solo hidrógeno. */
    val asksFuel: Boolean get() = this != ELECTRIC && this != FUEL_CELL

    companion object {
        fun fromCode(code: String): Powertrain? = decode(code, entries, OTHER) { it.code }
    }
}

/** Qué le echas. */
enum class Fuel(val code: String) {
    PETROL("petrol"), DIESEL("diesel"), LPG("lpg"), NATURAL_GAS("natural_gas"), HYDROGEN("hydrogen"), OTHER("other");

    companion object {
        fun fromCode(code: String): Fuel? = decode(code, entries, OTHER) { it.code }
    }
}

/**
 * Una paleta cerrada, para poder pintar una muestra que ayude a reconocerlo de lejos. Sin «otro»: todo
 * color de la paleta se puede pintar, y el que no está en ella no se guarda.
 */
enum class VehicleColor(val code: String) {
    WHITE("white"), BLACK("black"), GREY("grey"), SILVER("silver"), BLUE("blue"), RED("red"),
    GREEN("green"), YELLOW("yellow"), ORANGE("orange"), BROWN("brown"), PINK("pink"), PURPLE("purple");

    companion object {
        /** Sin «otro» al que recurrir, un código desconocido sale como sin color, `null`. */
        fun fromCode(code: String): VehicleColor? = entries.firstOrNull { it.code == code }
    }
}

/** La etiqueta ambiental de la DGT. No se calcula: la elige quien edita. */
enum class EmissionsLabel(val code: String) {
    ZERO("zero"), ECO("eco"), C("c"), B("b"), NONE("none");

    companion object {
        /** Aquí no hay «otro»: un código desconocido se trata como sin etiqueta conocida, `null`. */
        fun fromCode(code: String): EmissionsLabel? = entries.firstOrNull { it.code == code }
    }
}

/**
 * El combustible que corresponde a [powertrain], partiendo de [fuelCode]: lo que se guarda al cambiar
 * de sistema. Un eléctrico no lleva; uno de pila de combustible, hidrógeno; para el resto se conserva
 * si tiene sentido y se borra si no. Sin sistema elegido, el combustible va por su cuenta.
 */
fun fuelFor(powertrain: Powertrain?, fuelCode: String): String = when {
    powertrain == null -> fuelCode
    powertrain == Powertrain.ELECTRIC -> ""
    powertrain == Powertrain.FUEL_CELL -> Fuel.HYDROGEN.code
    Fuel.fromCode(fuelCode) in powertrain.allowedFuels -> fuelCode
    else -> ""
}

private inline fun <T> decode(code: String, values: List<T>, other: T, codeOf: (T) -> String): T? =
    if (code.isBlank()) null else values.firstOrNull { codeOf(it) == code } ?: other
