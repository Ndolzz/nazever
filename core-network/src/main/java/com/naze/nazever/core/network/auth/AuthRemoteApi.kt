package com.naze.nazever.core.network.auth

/**
 * Transport contract for Supabase Auth endpoints.
 */
interface AuthRemoteApi {
    suspend fun signUp(email: String, password: String): AuthTokenResponse
    suspend fun signInWithPassword(email: String, password: String): AuthTokenResponse
    suspend fun refresh(refreshToken: String): AuthTokenResponse
    suspend fun signOut(accessToken: String)
}

/**
 * HTTP-level failure. Carries only the status code; the response body is
 * deliberately dropped because it may echo submitted credentials.
 */
class AuthHttpException(val status: Int) :
    Exception("Supabase auth request failed with HTTP status " + status)
