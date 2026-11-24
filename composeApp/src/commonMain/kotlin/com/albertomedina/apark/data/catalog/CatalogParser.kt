package com.albertomedina.apark.data.catalog

import com.albertomedina.apark.domain.catalog.CatalogBrand
import com.albertomedina.apark.domain.catalog.VehicleCatalog
import com.albertomedina.apark.domain.catalog.foldForSearch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class BrandDto(val brand: String, val models: List<String> = emptyList())

@Serializable
private data class ExtraDto(val cars: List<BrandDto> = emptyList(), val motorcycles: List<String> = emptyList())

private val catalogJson = Json { ignoreUnknownKeys = true }

/**
 * Funde la lista de json-car-list ([carListJson], copiada sin tocar) con la nuestra ([extraJson]).
 *
 * Una marca que está en las dos se queda con la grafía de la primera y con los modelos de ambas, sin
 * repetir ninguno aunque cambien mayúsculas o tildes. Por eso la nuestra va en un fichero aparte:
 * se puede actualizar la suya sin pisar lo que le añadimos.
 */
fun parseCatalog(carListJson: String, extraJson: String): VehicleCatalog {
    val base = catalogJson.decodeFromString<List<BrandDto>>(carListJson)
    val extra = catalogJson.decodeFromString<ExtraDto>(extraJson)

    val merged = LinkedHashMap<String, CatalogBrand>()
    for (dto in base + extra.cars) {
        val name = dto.brand.trim()
        if (name.isEmpty()) continue
        val key = foldForSearch(name)
        val known = merged[key]
        merged[key] = CatalogBrand(
            name = known?.name ?: name,
            models = (known?.models.orEmpty() + dto.models.map { it.trim() }.filter { it.isNotEmpty() })
                .distinctBy { foldForSearch(it) }
        )
    }

    return VehicleCatalog(
        carBrands = merged.values
            .map { it.copy(models = it.models.sortedBy { model -> foldForSearch(model) }) }
            .sortedBy { foldForSearch(it.name) },
        motorcycleBrands = extra.motorcycles
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { foldForSearch(it) }
            .sortedBy { foldForSearch(it) }
    )
}
