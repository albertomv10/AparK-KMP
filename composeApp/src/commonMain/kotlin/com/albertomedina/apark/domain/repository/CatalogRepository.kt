package com.albertomedina.apark.domain.repository

import com.albertomedina.apark.domain.catalog.VehicleCatalog

/** La lista de marcas y modelos (spec 015). Viaja dentro de la app: no necesita red. */
interface CatalogRepository {
    suspend fun getCatalog(): VehicleCatalog
}
