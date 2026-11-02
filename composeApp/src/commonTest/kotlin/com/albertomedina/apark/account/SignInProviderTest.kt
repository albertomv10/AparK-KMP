package com.albertomedina.apark.account

import com.albertomedina.apark.domain.auth.SignInProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignInProviderTest {

    @Test
    fun `una cuenta de email y contraseña es PASSWORD`() {
        assertEquals(SignInProvider.PASSWORD, SignInProvider.from(listOf("password")))
    }

    @Test
    fun `Google y Apple se reconocen`() {
        assertEquals(SignInProvider.GOOGLE, SignInProvider.from(listOf("google.com")))
        assertEquals(SignInProvider.APPLE, SignInProvider.from(listOf("apple.com")))
    }

    @Test
    fun `si hay contraseña manda aunque haya otro proveedor enlazado`() {
        assertEquals(SignInProvider.PASSWORD, SignInProvider.from(listOf("google.com", "password")))
    }

    @Test
    fun `solo con contraseña se pueden cambiar email y contraseña`() {
        assertTrue(SignInProvider.PASSWORD.canChangeCredentials)
        assertFalse(SignInProvider.GOOGLE.canChangeCredentials)
        assertFalse(SignInProvider.APPLE.canChangeCredentials)
    }
}
