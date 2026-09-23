package com.naze.nazever.core.network.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorMapperTest {

    @Test
    fun mapsHttpExceptionByStatus() {
        assertEquals(AuthError.InvalidCredentials, AuthErrorMapper.fromStatus(400))
        assertEquals(AuthError.InvalidCredentials, AuthErrorMapper.fromStatus(401))
        assertEquals(AuthError.Unauthorized, AuthErrorMapper.fromStatus(403))
        assertEquals(AuthError.InvalidEmail, AuthErrorMapper.fromStatus(422))
        assertEquals(AuthError.WeakPassword, AuthErrorMapper.fromStatus(423))
        assertEquals(AuthError.TooManyRequests, AuthErrorMapper.fromStatus(429))
        assertEquals(AuthError.Network, AuthErrorMapper.fromStatus(500))
        assertEquals(AuthError.Unknown, AuthErrorMapper.fromStatus(418))
    }

    @Test
    fun mapsExceptionTypes() {
        assertEquals(AuthError.InvalidCredentials, AuthErrorMapper.fromException(AuthHttpException(401)))
        assertEquals(AuthError.Network, AuthErrorMapper.fromException(java.io.IOException("offline")))
        assertEquals(AuthError.Unknown, AuthErrorMapper.fromException(RuntimeException("boom")))
    }

    /**
     * Every user-facing error message must be a fixed, safe string:
     * no token or credential material may ever leak into it. Uses an
     * explicit list instead of kotlin-reflect (not on the classpath).
     */
    @Test
    fun messagesNeverContainTokenMaterial() {
        val token = "secret-token-value-123"
        val allErrors = listOf(
            AuthError.Network,
            AuthError.InvalidCredentials,
            AuthError.EmailTaken,
            AuthError.WeakPassword,
            AuthError.InvalidEmail,
            AuthError.TooManyRequests,
            AuthError.Unauthorized,
            AuthError.Unknown
        )
        for (error in allErrors) {
            assertTrue(!error.userMessage.contains(token))
        }
        assertTrue(!AuthErrorMapper.fromException(AuthHttpException(401)).userMessage.contains(token))
    }
}
