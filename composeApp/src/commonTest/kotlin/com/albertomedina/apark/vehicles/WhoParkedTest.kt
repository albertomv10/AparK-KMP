package com.albertomedina.apark.vehicles

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.presentation.components.WhoParked
import com.albertomedina.apark.presentation.components.whoParked
import kotlin.test.Test
import kotlin.test.assertEquals

class WhoParkedTest {

    private fun parkedBy(uid: String, name: String = "", email: String = "") =
        Vehicle.LocationModel(latitude = 1.0, timestamp = 5, user = Vehicle.ParkedBy(uid, name, email))

    @Test
    fun `si aparqué yo la tarjeta dice por ti y no mi nombre`() {
        assertEquals(WhoParked.You, whoParked(parkedBy("me", name = "Alberto"), currentUserId = "me"))
    }

    @Test
    fun `otro miembro sale por su nombre`() {
        assertEquals(WhoParked.Named("Edu"), whoParked(parkedBy("edu", name = "Edu", email = "edu@x.es"), "me"))
    }

    @Test
    fun `sin nombre sale su email`() {
        assertEquals(WhoParked.Named("edu@x.es"), whoParked(parkedBy("edu", email = "edu@x.es"), "me"))
    }

    @Test
    fun `una cuenta borrada no deja a nadie a quien nombrar`() {
        assertEquals(WhoParked.Unknown, whoParked(Vehicle.LocationModel(latitude = 1.0, timestamp = 5), "me"))
        assertEquals(WhoParked.Unknown, whoParked(parkedBy("edu"), "me"))
    }

    @Test
    fun `sin sesión nadie es yo`() {
        assertEquals(WhoParked.Named("Edu"), whoParked(parkedBy("", name = "Edu"), currentUserId = null))
    }
}
