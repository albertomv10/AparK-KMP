package com.albertomedina.apark.vehicledata

import com.albertomedina.apark.data.catalog.parseCatalog
import com.albertomedina.apark.domain.catalog.foldForSearch
import com.albertomedina.apark.domain.catalog.searchNames
import com.albertomedina.apark.domain.model.VehicleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VehicleCatalogTest {

    private val carList = """
        [
          { "brand": "Škoda", "models": ["Octavia", "Fabia", "Superb Combi"] },
          { "brand": "Citroën", "models": ["C3", "ë-C4"] },
          { "brand": "Mercedes-Benz", "models": ["A-Class"] },
          { "brand": "BMW", "models": ["X1", "Serie 3"] },
          { "brand": "Seat", "models": ["Leon", "Ibiza"] }
        ]
    """.trimIndent()

    private val extra = """
        {
          "cars": [
            { "brand": "Cupra", "models": ["Born", "Formentor"] },
            { "brand": "skoda", "models": ["Kamiq", "octavia"] }
          ],
          "motorcycles": ["Yamaha", "BMW", "Honda", "  "]
        }
    """.trimIndent()

    private val catalog = parseCatalog(carList, extra)

    @Test
    fun `quitar tildes y mayúsculas`() {
        assertEquals("skoda", foldForSearch("Škoda"))
        assertEquals("citroen", foldForSearch("CITROËN"))
        assertEquals("mercedes benz", foldForSearch(" Mercedes-Benz "))
        assertEquals("e c4", foldForSearch("ë-C4"))
    }

    @Test
    fun `skoda encuentra Škoda`() {
        assertEquals(listOf("Škoda"), searchNames("skoda", catalog.brandsFor(VehicleType.CAR)))
        assertEquals(listOf("Škoda"), searchNames("SKO", catalog.brandsFor(VehicleType.CAR)))
    }

    @Test
    fun `primero lo que empieza por lo escrito y después lo que lo contiene`() {
        val names = listOf("Abarth", "Seat", "Mercedes-Benz", "Benelli", "Ssangyong Benz")
        assertEquals(listOf("Benelli", "Mercedes-Benz", "Ssangyong Benz"), searchNames("ben", names))
        assertEquals(listOf("Abarth", "Seat"), searchNames("a", listOf("Seat", "Abarth")))
    }

    @Test
    fun `sin nada escrito no se sugiere nada`() {
        assertTrue(searchNames("", catalog.brandsFor(null)).isEmpty())
        assertTrue(searchNames("   ", catalog.brandsFor(null)).isEmpty())
    }

    @Test
    fun `como mucho seis sugerencias`() {
        val names = (1..20).map { "Marca $it" }
        assertEquals(6, searchNames("marca", names).size)
    }

    @Test
    fun `una marca que está en las dos listas suma sus modelos sin repetir`() {
        val skoda = catalog.carBrands.single { it.name == "Škoda" }
        assertEquals(listOf("Fabia", "Kamiq", "Octavia", "Superb Combi"), skoda.models)
    }

    @Test
    fun `las marcas que faltaban llegan de la nuestra`() {
        assertEquals(listOf("Born", "Formentor"), catalog.modelsOf("Cupra", VehicleType.CAR))
    }

    @Test
    fun `el modelo filtra por la marca aunque esté escrita a mano sin tilde`() {
        assertEquals(listOf("Fabia", "Kamiq", "Octavia", "Superb Combi"), catalog.modelsOf("skoda", VehicleType.CAR))
        assertTrue(catalog.modelsOf("Lada", VehicleType.CAR).isEmpty())
    }

    @Test
    fun `cada tipo busca en sus marcas`() {
        assertEquals(listOf("BMW", "Citroën", "Cupra", "Mercedes-Benz", "Seat", "Škoda"), catalog.brandsFor(VehicleType.CAR))
        assertEquals(catalog.brandsFor(VehicleType.CAR), catalog.brandsFor(VehicleType.VAN))
        assertEquals(catalog.brandsFor(VehicleType.CAR), catalog.brandsFor(VehicleType.MOTORHOME))
        assertEquals(listOf("BMW", "Honda", "Yamaha"), catalog.brandsFor(VehicleType.MOTORCYCLE))
    }

    @Test
    fun `sin tipo se busca en todas sin repetir las que están en las dos`() {
        val all = catalog.brandsFor(null)
        assertEquals(1, all.count { it == "BMW" })
        assertTrue("Yamaha" in all && "Škoda" in all)
        assertEquals(all, catalog.brandsFor(VehicleType.OTHER))
    }

    @Test
    fun `las motos no traen modelos`() {
        assertTrue(catalog.modelsOf("BMW", VehicleType.MOTORCYCLE).isEmpty())
        assertEquals(listOf("Serie 3", "X1"), catalog.modelsOf("BMW", null))
    }
}
