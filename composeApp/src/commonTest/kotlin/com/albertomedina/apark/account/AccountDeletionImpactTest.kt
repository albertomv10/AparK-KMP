package com.albertomedina.apark.account

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.domain.usecase.accountDeletionImpact
import kotlin.test.Test
import kotlin.test.assertEquals

/** Tiene que contar lo mismo que hará `planVehicle` en el servidor. */
class AccountDeletionImpactTest {

    private fun vehicle(name: String, owner: String, vararg members: String) =
        Vehicle(name = name, ownerId = owner, memberIds = members.toList())

    @Test
    fun `cada vehículo va a su sitio`() {
        val impact = accountDeletionImpact(
            listOf(
                vehicle("Solo mío", "me", "me"),
                vehicle("Compartido mío", "me", "me", "a"),
                vehicle("De otro", "a", "a", "me")
            ),
            uid = "me"
        )
        assertEquals(listOf("Solo mío"), impact.deleted)
        assertEquals(listOf("Compartido mío"), impact.handedOver)
        assertEquals(listOf("De otro"), impact.left)
    }

    @Test
    fun `sin vehículos no hay nada que contar`() {
        val impact = accountDeletionImpact(emptyList(), uid = "me")
        assertEquals(Triple(emptyList(), emptyList(), emptyList()), Triple(impact.deleted, impact.handedOver, impact.left))
    }
}
