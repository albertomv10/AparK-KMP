package com.albertomedina.apark.account

import com.albertomedina.apark.domain.usecase.USER_NAME_MAX_LENGTH
import com.albertomedina.apark.domain.usecase.UserNameError
import com.albertomedina.apark.domain.usecase.validateUserName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** El tope tiene que coincidir con `firestore.rules`: si no, la app aceptaría lo que la regla niega. */
class UserNameTest {

    @Test
    fun `vacío o solo espacios no vale`() {
        assertEquals(UserNameError.EMPTY, validateUserName(""))
        assertEquals(UserNameError.EMPTY, validateUserName("   "))
    }

    @Test
    fun `el tope es 40 como en las reglas`() {
        assertEquals(40, USER_NAME_MAX_LENGTH)
        assertNull(validateUserName("x".repeat(40)))
        assertEquals(UserNameError.TOO_LONG, validateUserName("x".repeat(41)))
    }

    @Test
    fun `los espacios de los extremos no cuentan`() {
        assertNull(validateUserName("  Eduardo  "))
        assertNull(validateUserName(" " + "x".repeat(40) + " "))
    }
}
