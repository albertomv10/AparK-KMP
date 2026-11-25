package com.albertomedina.apark.vehicles

import com.albertomedina.apark.domain.model.Vehicle
import com.albertomedina.apark.presentation.vehicles.inOrder
import com.albertomedina.apark.presentation.vehicles.moved
import kotlin.test.Test
import kotlin.test.assertEquals

class VehicleOrderTest {

    private fun vehicles(vararg ids: String) = ids.map { Vehicle(id = it, name = it) }
    private fun List<Vehicle>.ids() = map { it.id }

    @Test
    fun `los vehículos salen en el orden pedido`() {
        assertEquals(listOf("c", "a", "b"), vehicles("a", "b", "c").inOrder(listOf("c", "a", "b")).ids())
    }

    @Test
    fun `un vehículo que entra mientras se ordena sale al final sin perderse`() {
        val arrived = vehicles("a", "nuevo", "b", "c")
        assertEquals(listOf("c", "b", "a", "nuevo"), arrived.inOrder(listOf("c", "b", "a")).ids())
    }

    @Test
    fun `un vehículo que ya no está no deja hueco`() {
        assertEquals(listOf("c", "a"), vehicles("a", "c").inOrder(listOf("c", "borrado", "a")).ids())
    }

    @Test
    fun `sin cambios la lista queda igual`() {
        val list = vehicles("a", "b", "c")
        assertEquals(list, list.inOrder(list.ids()))
    }

    @Test
    fun `mover lleva el elemento a su sitio en un solo paso`() {
        assertEquals(listOf("d", "a", "b", "c"), listOf("a", "b", "c", "d").moved(from = 3, to = 0))
        assertEquals(listOf("b", "c", "a", "d"), listOf("a", "b", "c", "d").moved(from = 0, to = 2))
    }

    @Test
    fun `mover fuera de rango no cambia nada`() {
        val list = listOf("a", "b")
        assertEquals(list, list.moved(from = 0, to = 5))
        assertEquals(list, list.moved(from = -1, to = 0))
        assertEquals(list, list.moved(from = 1, to = 1))
    }
}
