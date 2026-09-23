package com.naze.nazever.core.security

/**
 * Encodes/decodes a [SessionData] to a single string suitable for
 * encrypted storage.
 *
 * Format v2: <payload><SEP><checksum> where payload fields are joined
 * with a low control character and the checksum is FNV-1a.
 * Decoding is strict: any mismatch, truncation or corruption results in
 * null instead of an exception, so callers can self-heal. Sessions
 * stored in the older v1 format (pre device-management) decode to null,
 * which self-heals to a clean re-login.
 */
object SessionCodec {

    private const val VERSION = "v2"
    private const val FIELD_SEP = '\u0001'
    private const val CHECKSUM_SEP = '\u0002'

    fun encode(session: SessionData): String {
        val payload = listOf(
            VERSION,
            session.accessToken,
            session.refreshToken,
            session.expiresAtMillis.toString(),
            session.userId,
            session.email ?: "",
            session.sessionId ?: "",
            session.deviceId ?: ""
        ).joinToString(FIELD_SEP.toString())
        val checksum = fnv1a(payload)
        return payload + CHECKSUM_SEP + checksum.toString(16)
    }

    fun decode(raw: String?): SessionData? {
        if (raw == null || raw.isEmpty()) return null
        return try {
            val sepIndex = raw.indexOf(CHECKSUM_SEP)
            if (sepIndex < 0) return null
            val payload = raw.substring(0, sepIndex)
            val checksum = raw.substring(sepIndex + 1)
            if (fnv1a(payload).toString(16) != checksum) return null
            val fields = payload.split(FIELD_SEP.toString())
            if (fields.size != 8) return null
            if (fields[0] != VERSION) return null
            val expiresAt = fields[3].toLong()
            SessionData(
                accessToken = fields[1],
                refreshToken = fields[2],
                expiresAtMillis = expiresAt,
                userId = fields[4],
                email = if (fields[5].isEmpty()) null else fields[5],
                sessionId = if (fields[6].isEmpty()) null else fields[6],
                deviceId = if (fields[7].isEmpty()) null else fields[7]
            )
        } catch (t: Throwable) {
            null
        }
    }

    private fun fnv1a(input: String): Long {
        var hash = 0xcbf29ce484222325L
        for (c in input) {
            hash = hash xor c.code.toLong()
            hash *= 0x100000001b3L
        }
        return hash
    }
}
