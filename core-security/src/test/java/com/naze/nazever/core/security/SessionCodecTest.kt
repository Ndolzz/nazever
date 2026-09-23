package com.naze.nazever.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCodecTest {

    private val session = SessionData(
        accessToken = "access-token-value",
        refreshToken = "refresh-token-value",
        expiresAtMillis = 123456789L,
        userId = "user-1",
        email = "a@example.com"
    )

    @Test
    fun encodeDecodeRoundTrip() {
        val decoded = SessionCodec.decode(SessionCodec.encode(session))
        assertEquals(session, decoded)
    }

    @Test
    fun decodeNullAndEmptyReturnsNull() {
        assertNull(SessionCodec.decode(null))
        assertNull(SessionCodec.decode(""))
    }

    @Test
    fun decodeCorruptedChecksumReturnsNull() {
        val encoded = SessionCodec.encode(session)
        val corrupted = encoded.substring(0, encoded.length - 1) + "0"
        assertNull(SessionCodec.decode(corrupted))
    }

    @Test
    fun decodeTruncatedReturnsNull() {
        val encoded = SessionCodec.encode(session)
        assertNull(SessionCodec.decode(encoded.substring(0, encoded.length / 2)))
    }

    @Test
    fun decodeUnknownVersionReturnsNull() {
        val payload = "v9" + ('\u0001').toString() + "a" + ('\u0001').toString() + "b" +
            ('\u0001').toString() + "1" + ('\u0001').toString() + "u" + ('\u0001').toString() + "e"
        assertNull(SessionCodec.decode("v9" + ('\u0001').toString() + "x"))
    }

    @Test
    fun updateReplacesOldSession() {
        val first = SessionCodec.encode(session)
        val updated = session.copy(accessToken = "new-access-token")
        val second = SessionCodec.encode(updated)
        assertNotEquals(first, second)
        assertEquals(updated, SessionCodec.decode(second))
    }
}
