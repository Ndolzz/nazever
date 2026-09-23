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
        email = "a@example.com",
        sessionId = "session-1",
        deviceId = "device-1"
    )

    @Test
    fun encodeDecodeRoundTripWithSessionIdentifiers() {
        val decoded = SessionCodec.decode(SessionCodec.encode(session))
        assertEquals(session, decoded)
    }

    @Test
    fun encodeDecodeRoundTripWithoutSessionIdentifiers() {
        val legacy = session.copy(sessionId = null, deviceId = null)
        val decoded = SessionCodec.decode(SessionCodec.encode(legacy))
        assertEquals(legacy, decoded)
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
    fun decodeTamperedPayloadReturnsNull() {
        val encoded = SessionCodec.encode(session)
        val sepIndex = encoded.indexOf(2.toChar())
        val payload = encoded.substring(0, sepIndex) + "x"
        val checksum = encoded.substring(sepIndex + 1)
        assertNull(SessionCodec.decode(payload + (2.toChar()).toString() + checksum))
    }

    @Test
    fun decodeTruncatedReturnsNull() {
        val encoded = SessionCodec.encode(session)
        assertNull(SessionCodec.decode(encoded.substring(0, encoded.length / 2)))
    }

    @Test
    fun decodeGarbageReturnsNull() {
        assertNull(SessionCodec.decode("v1" + (1.toChar()).toString() + "x"))
        assertNull(SessionCodec.decode("v9" + (1.toChar()).toString() + "x"))
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
