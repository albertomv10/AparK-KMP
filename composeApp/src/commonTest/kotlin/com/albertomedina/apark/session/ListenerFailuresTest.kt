package com.albertomedina.apark.session

import com.albertomedina.apark.utils.catchListenerFailure
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Spec 021: el error de una escucha nunca se escapa, y solo cuenta como fallo con la sesión viva. */
class ListenerFailuresTest {

    private val denied = IllegalStateException("PERMISSION_DENIED")

    /** Una escucha que entrega dos valores y luego falla, como una a la que le quitan el permiso. */
    private val listener = flow {
        emit(1)
        emit(2)
        throw denied
    }

    @Test
    fun `con la sesión viva el fallo se entrega una vez y no se escapa`() = runTest {
        val failures = mutableListOf<Throwable>()
        val values = listener.catchListenerFailure(isSignedIn = { true }) { failures += it }.toList()
        assertEquals(listOf(1, 2), values)
        assertEquals(listOf<Throwable>(denied), failures)
    }

    @Test
    fun `si la sesión terminó no es un fallo y tampoco se escapa`() = runTest {
        val failures = mutableListOf<Throwable>()
        val values = listener.catchListenerFailure(isSignedIn = { false }) { failures += it }.toList()
        assertEquals(listOf(1, 2), values)
        assertTrue(failures.isEmpty())
    }
}
