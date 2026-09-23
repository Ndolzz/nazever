package com.naze.nazever.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Production [SessionStore] backed by EncryptedSharedPreferences.
 *
 * - Keys are encrypted with AES256-SIV, values with AES256-GCM.
 * - The master key lives in the Android Keystore.
 * - Corrupted or unreadable data never crashes the app: reads degrade
 *   to "no session" and the stored value is cleared so it self-heals.
 */
class SecureSessionStore(
    context: Context,
    fileName: String = "nazever_secure_session"
) : SessionStore {

    private val appContext = context.applicationContext
    private var prefs: SharedPreferences? = createPrefs(fileName)

    private fun createPrefs(fileName: String): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (t: Throwable) {
            null
        }
    }

    override fun save(session: SessionData) {
        val raw = SessionCodec.encode(session)
        prefsOrThrow().edit().putString(KEY_SESSION, raw).commit()
    }

    override fun read(): SessionData? {
        val current = prefs ?: return null
        val raw = try {
            current.getString(KEY_SESSION, null)
        } catch (t: Throwable) {
            null
        }
        if (raw == null) return null
        val session = SessionCodec.decode(raw)
        if (session == null) {
            // Corrupted entry: clear so the next write starts clean.
            clear()
        }
        return session
    }

    override fun clear() {
        val current = prefs ?: return
        try {
            current.edit().remove(KEY_SESSION).commit()
        } catch (t: Throwable) {
            // Nothing sensible to do; storage is unusable anyway.
        }
    }

    override fun hasSession(): Boolean = read() != null

    private fun prefsOrThrow(): SharedPreferences {
        // Retry creation once (e.g. Keystore recovered after process restart).
        prefs?.let { return it }
        val created = createPrefs(pendingFileName)
        if (created != null) {
            prefs = created
            return created
        }
        throw IllegalStateException("Secure session storage unavailable")
    }

    private val pendingFileName: String get() = "nazever_secure_session"

    companion object {
        private const val KEY_SESSION = "session_v1"
    }
}
