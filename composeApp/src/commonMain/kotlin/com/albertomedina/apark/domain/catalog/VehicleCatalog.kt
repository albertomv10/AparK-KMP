package com.albertomedina.apark.domain.catalog

import com.albertomedina.apark.domain.model.VehicleType

/** Una marca de coche con sus modelos, tal como los sugiere el buscador. */
data class CatalogBrand(val name: String, val models: List<String>)

/**
 * La lista de marcas y modelos que ayuda a rellenar el vehículo (spec 015). **Solo sugiere**: lo que
 * se guarda es el texto del campo, venga de aquí o escrito a mano, y nada impide guardar lo que no está.
 */
class VehicleCatalog(
    val carBrands: List<CatalogBrand>,
    val motorcycleBrands: List<String>
) {
    private val carBrandsByKey: Map<String, CatalogBrand> = carBrands.associateBy { foldForSearch(it.name) }

    /**
     * Dónde buscar la marca según el tipo: coche, furgoneta y autocaravana, en las de coche; moto, en
     * las de moto; sin tipo u «otro», en todas, sin repetir las que están en las dos (BMW, Peugeot…).
     */
    fun brandsFor(type: VehicleType?): List<String> = when (type) {
        VehicleType.CAR, VehicleType.VAN, VehicleType.MOTORHOME -> carBrands.map { it.name }
        VehicleType.MOTORCYCLE -> motorcycleBrands
        VehicleType.OTHER, null -> (carBrands.map { it.name } + motorcycleBrands)
            .distinctBy { foldForSearch(it) }
            .sortedBy { foldForSearch(it) }
    }

    /**
     * Los modelos de [brand], comparando sin mayúsculas ni tildes: «skoda» escrito a mano también tiene
     * los de Škoda. Las motos no traen modelos, así que con el tipo moto no hay nada que sugerir.
     */
    fun modelsOf(brand: String, type: VehicleType?): List<String> =
        if (type == VehicleType.MOTORCYCLE) emptyList()
        else carBrandsByKey[foldForSearch(brand)]?.models.orEmpty()

    companion object {
        val EMPTY = VehicleCatalog(emptyList(), emptyList())
    }
}
