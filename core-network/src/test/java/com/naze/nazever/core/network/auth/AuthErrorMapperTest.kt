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

    @Test
    fun messagesNeverContainTokenMaterial() {
        val token = "secret-token-value-123"
        val error = AuthErrorMapper.fromException(AuthHttpException(401))
        for (values in AuthError::class.sealedSubclasses) {
            val instance = values.objectInstance
            if (instance != null) {
                assertTrue(!instance.userMessage.contains(token))
            }
        }
        assertTrue(!error.userMessage.contains(token))
    }
}
