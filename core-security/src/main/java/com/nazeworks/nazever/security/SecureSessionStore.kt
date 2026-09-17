package com.nazeworks.nazever.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Satu-satunya tempat access token / refresh token Supabase disimpan
 * di perangkat. Aturan wajib:
 *
 *  - TIDAK PERNAH di-Log.d/Log.e dengan isi token.
 *  - TIDAK PERNAH disimpan sebagai SharedPreferences biasa.
 *  - Key AES-256-GCM dibangkitkan & disimpan di Android Keystore
 *    (StrongBox jika tersedia), tidak pernah di file.
 *  - `clear()` wajib dipanggil saat sign-out di device ini, dan saat
 *    server melaporkan session sudah di-revoke dari device lain.
 */
class SecureSessionStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setRequestStrongBoxBacked(true) // fallback otomatis jika device tidak punya StrongBox
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(accessToken: String, refreshToken: String, expiresAtEpochSeconds: Long) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAtEpochSeconds)
            .apply()
    }

    fun accessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun refreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun expiresAtEpochSeconds(): Long = prefs.getLong(KEY_EXPIRES_AT, 0L)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val FILE_NAME = "nazever_secure_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}
