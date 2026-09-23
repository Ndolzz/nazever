package com.naze.nazever.core.network.auth

import com.naze.nazever.core.network.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Ktor-based client for the official Supabase Auth REST endpoints:
 * - POST /auth/v1/signup
 * - POST /auth/v1/token?grant_type=password
 * - POST /auth/v1/token?grant_type=refresh_token
 * - POST /auth/v1/logout
 *
 * Configuration (URL + public anon key) is injected; nothing is hardcoded.
 */
class SupabaseAuthRemoteApi(
    private val baseUrl: String,
    private val anonKey: String,
    engine: HttpClientEngine? = null
) : AuthRemoteApi {

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

    override suspend fun signUp(email: String, password: String): AuthTokenResponse {
        return postToken("signup", SignUpRequest(email, password))
    }

    override suspend fun signInWithPassword(email: String, password: String): AuthTokenResponse {
        return postToken("token?grant_type=password", PasswordGrant(email, password))
    }

    override suspend fun refresh(refreshToken: String): AuthTokenResponse {
        return postToken("token?grant_type=refresh_token", RefreshGrant(refreshToken))
    }

    override suspend fun signOut(accessToken: String) {
        wrap {
            client.post(url("logout")) {
                headers(accessToken)
            }
        }
    }

    private suspend fun postToken(path: String, body: Any): AuthTokenResponse {
        return wrap {
            client.post(url(path)) {
                headers(null)
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        }
    }

    private suspend fun <T> wrap(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: io.ktor.client.plugins.ClientRequestException) {
            throw AuthHttpException(e.response.status.value)
        } catch (e: io.ktor.client.plugins.RedirectResponseException) {
            throw AuthHttpException(e.response.status.value)
        } catch (e: io.ktor.client.plugins.ServerResponseException) {
            throw AuthHttpException(e.response.status.value)
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.headers(accessToken: String?) {
        header("apikey", anonKey)
        if (accessToken != null) {
            header(HttpHeaders.Authorization, "Bearer " + accessToken)
        }
    }

    private fun url(path: String): String = baseUrl.trimEnd('/') + "/auth/v1/" + path

    companion object {
        /** Creates a client from BuildConfig values supplied via gradle.properties. */
        fun fromBuildConfig(): SupabaseAuthRemoteApi = SupabaseAuthRemoteApi(
            baseUrl = BuildConfig.SUPABASE_URL,
            anonKey = BuildConfig.SUPABASE_ANON_KEY
        )
    }
}
