package com.naze.nazever.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthFormValidatorTest {

    @Test
    fun validEmailPasses() {
        assertNull(AuthFormValidator.validateEmail("user@example.com"))
        assertNull(AuthFormValidator.validateEmail("a.b+c@sub.domain.co.id"))
    }

    @Test
    fun invalidEmailFails() {
        assertNotNull(AuthFormValidator.validateEmail(""))
        assertNotNull(AuthFormValidator.validateEmail("not-an-email"))
        assertNotNull(AuthFormValidator.validateEmail("missing@tld"))
    }

    @Test
    fun validPasswordPasses() {
        assertNull(AuthFormValidator.validatePassword("abcdef"))
        assertNull(AuthFormValidator.validatePassword("rahasia-kuat-123"))
    }

    @Test
    fun weakPasswordFails() {
        assertNotNull(AuthFormValidator.validatePassword(""))
        assertNotNull(AuthFormValidator.validatePassword("abc"))
        assertEquals(
            "Kata sandi minimal " + AuthFormValidator.MIN_PASSWORD_LENGTH + " karakter.",
            AuthFormValidator.validatePassword("abc")
        )
    }

    @Test
    fun confirmMismatchFails() {
        assertNull(AuthFormValidator.validateConfirm("abcdef", "abcdef"))
        assertNotNull(AuthFormValidator.validateConfirm("abcdef", ""))
        assertEquals(
            "Konfirmasi kata sandi tidak sama.",
            AuthFormValidator.validateConfirm("abcdef", "abcdeg")
        )
    }
}
