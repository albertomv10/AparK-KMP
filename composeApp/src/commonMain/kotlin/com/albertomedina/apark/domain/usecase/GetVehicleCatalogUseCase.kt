package com.albertomedina.apark.domain.usecase

import com.albertomedina.apark.domain.catalog.VehicleCatalog
import com.albertomedina.apark.domain.repository.CatalogRepository
import kotlin.coroutines.cancellation.CancellationException

/** La lista de marcas y modelos. Si no se pudiera leer, el formulario sigue funcionando a mano. */
class GetVehicleCatalogUseCase(
    private val repository: CatalogRepository
) {
    suspend operator fun invoke(): VehicleCatalog = try {
        repository.getCatalog()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        VehicleCatalog.EMPTY
    }
}
