package com.albertomedina.apark.data.catalog

import apark.composeapp.generated.resources.Res
import com.albertomedina.apark.domain.catalog.VehicleCatalog
import com.albertomedina.apark.domain.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Lee el catálogo de `composeResources/files/catalog/` **una sola vez por proceso** y lo guarda en
 * memoria: es un `single` de Koin, y abrir el formulario otra vez no vuelve a leer ni a parsear.
 * Junto a los JSON va `NOTICE.txt`, el aviso de la licencia MIT de json-car-list.
 */
class ResourceCatalogRepository : CatalogRepository {
    private val mutex = Mutex()
    private var cached: VehicleCatalog? = null

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun getCatalog(): VehicleCatalog = mutex.withLock {
        cached ?: run {
            val carList = Res.readBytes("files/catalog/car-list.json").decodeToString()
            val extra = Res.readBytes("files/catalog/extra.json").decodeToString()
            withContext(Dispatchers.Default) { parseCatalog(carList, extra) }
        }.also { cached = it }
    }
}
