package com.albertomedina.apark.vehicledetail

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.model.VehicleMember
import com.albertomedina.apark.presentation.vehicledetail.memberRows
import com.albertomedina.apark.presentation.vehicledetail.membersNeedRefresh
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VehicleDetailLogicTest {

    private val base = Vehicle(id = "v", name = "Coche", ownerId = "me", memberIds = listOf("me", "edu"))

    @Test
    fun `la primera vez siempre se piden los miembros`() {
        assertTrue(membersNeedRefresh(previous = null, current = base))
    }

    @Test
    fun `aparcar no cambia los miembros y no merece una llamada`() {
        val parked = base.copy(lastLocation = Vehicle.LocationModel(latitude = 1.0, timestamp = 5), updatedAt = 9)
        assertFalse(membersNeedRefresh(base, parked))
    }

    @Test
    fun `entrar o salir o pasar la propiedad sí la merecen`() {
        assertTrue(membersNeedRefresh(base, base.copy(memberIds = listOf("me"))))
        assertTrue(membersNeedRefresh(base, base.copy(memberIds = listOf("me", "edu", "ana"))))
        assertTrue(membersNeedRefresh(base, base.copy(ownerId = "edu")))
    }

    private fun member(uid: String, name: String = uid, isOwner: Boolean = false) = VehicleMember(uid, name, isOwner)

    @Test
    fun `el dueño primero y el resto en el orden en que llegan con yo marcado`() {
        val rows = memberRows(listOf(member("ana"), member("me"), member("edu")), ownerId = "me", currentUserId = "edu")
        assertEquals(listOf("me", "ana", "edu"), rows.map { it.member.uid })
        assertEquals(listOf(true, false, false), rows.map { it.isOwner })
        assertEquals(listOf(false, false, true), rows.map { it.isMe })
    }

    @Test
    fun `el dueño sale del vehículo en tiempo real y no de la lista que llega después`() {
        // La lista aún dice que el dueño es «me», pero la propiedad ya ha pasado a «edu».
        val stale = listOf(member("me", isOwner = true), member("edu"))
        val rows = memberRows(stale, ownerId = "edu", currentUserId = "me")
        assertEquals("edu", rows.first().member.uid)
        assertTrue(rows.first().isOwner)
        assertFalse(rows.single { it.member.uid == "me" }.isOwner)
    }
}
