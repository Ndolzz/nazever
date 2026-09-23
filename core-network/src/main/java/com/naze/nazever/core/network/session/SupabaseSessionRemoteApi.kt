package com.naze.nazever.core.network.session

import com.naze.nazever.core.network.BuildConfig
import com.naze.nazever.core.network.auth.AuthHttpException
import com.naze.nazever.core.network.auth.AuthTokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Ktor client for TASK-007 session management:
 * - Edge Functions: register_session, refresh_session, revoke_session.
 * - PostgREST (RLS-protected): sessions/devices reads for the caller only.
 */
class SupabaseSessionRemoteApi(
    private val baseUrl: String,
    private val anonKey: String,
    engine: HttpClientEngine? = null
) : SessionRemoteApi {

    private val client: HttpClient = if (engine != null) {
        HttpClient(engine) { configure() }
    } else {
        HttpClient(Android) { configure() }
    }

    private fun io.ktor.client.HttpClientConfig<*>.configure() {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    override suspend fun registerSession(
        accessToken: String,
        deviceName: String,
        platform: String,
        refreshToken: String
    ): RegisteredSession {
        val response: RegisterSessionResponse = wrap {
            client.post(url("functions/v1/register_session")) {
                headers(accessToken)
                contentType(ContentType.Application.Json)
                setBody(RegisterSessionRequest(deviceName, platform, refreshToken))
            }.body()
        }
        return RegisteredSession(response.sessionId, response.deviceId)
    }

    override suspend fun refreshSession(refreshToken: String): AuthTokenResponse {
        return wrap {
            client.post(url("functions/v1/refresh_session")) {
                header("apikey", anonKey)
                contentType(ContentType.Application.Json)
                setBody(RefreshSessionRequest(refreshToken))
            }.body()
        }
    }

    override suspend fun isSessionRevoked(accessToken: String, sessionId: String): Boolean {
        val rows: List<SessionStatusRow> = wrap {
            client.get(url("rest/v1/sessions?id=eq." + sessionId + "&select=id,revoked_at")) {
                headers(accessToken)
            }.body()
        }
        // Fail closed: a missing row means the session no longer exists
        // (or is not ours), so it must be treated as revoked.
        return rows.isEmpty() || rows.first().revokedAt != null
    }

    override suspend fun listDeviceSessions(accessToken: String): List<DeviceSessionInfo> {
        val rows: List<SessionListRow> = wrap {
            client.get(
                url(
                    "rest/v1/sessions?select=id,device_id,revoked_at,created_at," +
                        "devices(device_name,platform,last_active_at,created_at)" +
                        "&order=created_at.desc"
                )
            ) {
                headers(accessToken)
            }.body()
        }
        return rows.map { row ->
            DeviceSessionInfo(
                sessionId = row.id,
                deviceId = row.deviceId,
                deviceName = row.devices?.deviceName ?: "Perangkat tidak diketahui",
                platform = row.devices?.platform ?: "unknown",
                lastActiveAt = row.devices?.lastActiveAt,
                createdAt = row.createdAt,
                revokedAt = row.revokedAt
            )
        }
    }

    override suspend fun revokeSession(accessToken: String, sessionId: String) {
        wrap {
            client.post(url("functions/v1/revoke_session")) {
                headers(accessToken)
                contentType(ContentType.Application.Json)
                setBody(RevokeSessionRequest(sessionId))
            }
        }
    }

    private suspend fun <T> wrap(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: ClientRequestException) {
            throw AuthHttpException(e.response.status.value)
        } catch (e: RedirectResponseException) {
            throw AuthHttpException(e.response.status.value)
        } catch (e: ServerResponseException) {
            throw AuthHttpException(e.response.status.value)
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.headers(accessToken: String) {
        header("apikey", anonKey)
        header(HttpHeaders.Authorization, "Bearer " + accessToken)
    }

    private fun url(path: String): String = baseUrl.trimEnd('/') + "/" + path

    companion object {
        /** Creates a client from BuildConfig values supplied via gradle.properties. */
        fun fromBuildConfig(): SupabaseSessionRemoteApi = SupabaseSessionRemoteApi(
            baseUrl = BuildConfig.SUPABASE_URL,
            anonKey = BuildConfig.SUPABASE_ANON_KEY
        )
    }
}
