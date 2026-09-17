package com.nazeworks.nazever.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * PENTING — kunci tidak pernah hardcode di source code:
 *
 *  - `supabaseUrl` dan `supabaseAnonKey` dibaca dari BuildConfig, yang
 *    di-generate Gradle dari `local.properties` (developer lokal) atau
 *    dari secret CI (release build). Lihat app/build.gradle.kts.
 *  - `local.properties` WAJIB ada di .gitignore.
 *  - `anon key` Supabase memang didesain public (dipakai di client),
 *    tapi keamanan sesungguhnya datang dari Row Level Security (RLS)
 *    di database — bukan dari kerahasiaan anon key itu sendiri.
 *  - Service role key TIDAK PERNAH ikut dibundle ke APK. Operasi yang
 *    butuh service role (mis. konfirmasi pairing lintas-user) hanya
 *    boleh jalan di Supabase Edge Function, bukan di client.
 */
object SupabaseClientProvider {

    @Volatile
    private var client: SupabaseClient? = null

    fun get(supabaseUrl: String, supabaseAnonKey: String): SupabaseClient =
        client ?: synchronized(this) {
            client ?: createSupabaseClient(
                supabaseUrl = supabaseUrl,
                supabaseKey = supabaseAnonKey
            ) {
                install(Auth) {
                    // Auto refresh & persist session ditangani manual lewat
                    // SecureSessionStore (core-security), bukan storage default
                    // SDK, supaya token selalu lewat Android Keystore.
                    alwaysAutoRefresh = true
                }
                install(Postgrest)
                install(Realtime)
                install(Storage)
                install(Functions)
            }.also { client = it }
        }
}
