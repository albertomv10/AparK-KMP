package com.albertomedina.apark.vehicles

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.presentation.components.Who
import com.albertomedina.apark.presentation.components.shortParkedLine
import com.albertomedina.apark.presentation.components.whoIs
import com.albertomedina.apark.presentation.components.whoParked
import kotlin.test.Test
import kotlin.test.assertEquals

class WhoParkedTest {

    private fun parkedBy(uid: String, name: String = "", email: String = "") =
        Vehicle.LocationModel(latitude = 1.0, timestamp = 5, user = Vehicle.ParkedBy(uid, name, email))

    @Test
    fun `si aparqué yo la tarjeta dice por ti y no mi nombre`() {
        assertEquals(Who.You, whoParked(parkedBy("me", name = "Alberto"), currentUserId = "me"))
    }

    @Test
    fun `otro miembro sale por su nombre`() {
        assertEquals(Who.Named("Edu"), whoParked(parkedBy("edu", name = "Edu", email = "edu@x.es"), "me"))
    }

    @Test
    fun `sin nombre sale su email`() {
        assertEquals(Who.Named("edu@x.es"), whoParked(parkedBy("edu", email = "edu@x.es"), "me"))
    }

    @Test
    fun `una cuenta borrada no deja a nadie a quien nombrar`() {
        assertEquals(Who.Unknown, whoParked(Vehicle.LocationModel(latitude = 1.0, timestamp = 5), "me"))
        assertEquals(Who.Unknown, whoParked(parkedBy("edu"), "me"))
    }

    @Test
    fun `sin sesión nadie es yo`() {
        assertEquals(Who.Named("Edu"), whoParked(parkedBy("", name = "Edu"), currentUserId = null))
    }

    private val names = mapOf("edu" to "Edu", "ana" to "")

    @Test
    fun `en una incidencia el nombre sale de los miembros`() {
        assertEquals(Who.You, whoIs("me", currentUserId = "me", names = names))
        assertEquals(Who.Named("Edu"), whoIs("edu", currentUserId = "me", names = names))
    }

    @Test
    fun `quien ya no es miembro o no tiene nombre no se nombra`() {
        assertEquals(Who.Unknown, whoIs("se-fue", currentUserId = "me", names = names))
        assertEquals(Who.Unknown, whoIs("ana", currentUserId = "me", names = names))
        assertEquals(Who.Unknown, whoIs("", currentUserId = "me", names = names))
    }

    // La frase corta de la tarjeta del mapa (spec 022).

    @Test
    fun `en la tarjeta quien aparcó va detrás de un punto`() {
        assertEquals("Hace 5 min · Edu", shortParkedLine("Hace 5 min", Who.Named("Edu"), you = "tú"))
    }

    @Test
    fun `en la tarjeta si aparqué yo dice tú`() {
        assertEquals("Ayer a las 18:45 · tú", shortParkedLine("Ayer a las 18:45", Who.You, you = "tú"))
    }

    @Test
    fun `en la tarjeta sin nadie a quien nombrar solo va el cuándo`() {
        assertEquals("El 12 nov", shortParkedLine("El 12 nov", Who.Unknown, you = "tú"))
    }
}
