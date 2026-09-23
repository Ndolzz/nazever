package com.naze.nazever.core.network.auth

import com.naze.nazever.core.security.SessionData
import com.naze.nazever.core.security.SessionStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeSessionStore : SessionStore {
    var map: MutableMap<String, String> = mutableMapOf()
    private var session: SessionData? = null

    override fun save(session: SessionData) {
        // Simulate encrypted storage: value is stored, never plaintext.
        map["session_v1"] = "ENC(" + session.accessToken.hashCode() + ")"
        this.session = session
    }

    override fun read(): SessionData? = session

    override fun clear() {
        map.clear()
        session = null
    }

    override fun hasSession(): Boolean = session != null
}

class FakeRemoteApi : AuthRemoteApi {
    var signInCalls = 0
    var refreshCalls = 0
    var signOutCalls = 0
    var failSignInWith: Exception? = null
    var failRefreshWith: Exception? = null
    var failSignOutWith: Exception? = null

    override suspend fun signUp(email: String, password: String): AuthTokenResponse =
        signInWithPassword(email, password)

    override suspend fun signInWithPassword(email: String, password: String): AuthTokenResponse {
        failSignInWith?.let { throw it }
        signInCalls++
        return AuthTokenResponse(
            accessToken = "access-A",
            refreshToken = "refresh-A",
            expiresIn = 3600,
            user = AuthUser(id = "user-A", email = email)
        )
    }

    override suspend fun refresh(refreshToken: String): AuthTokenResponse {
        failRefreshWith?.let { throw it }
        refreshCalls++
        return AuthTokenResponse(
            accessToken = "access-B",
            refreshToken = "refresh-B",
            expiresIn = 3600,
            user = AuthUser(id = "user-A")
        )
    }

    override suspend fun signOut(accessToken: String) {
        failSignOutWith?.let { throw it }
        signOutCalls++
    }
}

class NazeVerAuthRepositoryTest {

    private val now = 1_000_000L

    private fun repo(
        store: SessionStore = FakeSessionStore(),
        api: AuthRemoteApi = FakeRemoteApi()
    ): NazeVerAuthRepository = NazeVerAuthRepository(api, store, { now })

    @Test
    fun signInSavesSession() = runBlocking {
        val store = FakeSessionStore()
        val result = repo(store).signIn("a@example.com", "password123")
        assertTrue(result is AuthResult.Success)
        assertEquals("user-A", (result as AuthResult.Success).session.userId)
        assertTrue(store.hasSession())
        assertEquals(now + 3600_000L, result.session.expiresAtMillis)
    }

    @Test
    fun signUpSavesSession() = runBlocking {
        val store = FakeSessionStore()
        val result = repo(store).signUp("a@example.com", "password123")
        assertTrue(result is AuthResult.Success)
        assertTrue(store.hasSession())
    }

    @Test
    fun signInFailureMapsErrorAndSavesNothing() = runBlocking {
        val api = FakeRemoteApi().apply { failSignInWith = AuthHttpException(401) }
        val store = FakeSessionStore()
        val result = repo(store, api).signIn("a@example.com", "wrong")
        assertTrue(result is AuthResult.Failure)
        assertEquals(AuthError.InvalidCredentials, (result as AuthResult.Failure).error)
        assertFalse(store.hasSession())
    }

    @Test
    fun signOutClearsStoreEvenWhenRemoteFails() = runBlocking {
        val api = FakeRemoteApi().apply { failSignOutWith = java.io.IOException("offline") }
        val store = FakeSessionStore()
        val repository = repo(store, api)
        repository.signIn("a@example.com", "password123")
        repository.signOut()
        assertFalse(store.hasSession())
    }

    @Test
    fun restoreSessionWithEmptyStoreReturnsNullWithoutCrash() = runBlocking {
        val store = FakeSessionStore()
        assertNull(repo(store).restoreSession())
    }

    @Test
    fun restoreSessionWithValidTokenReturnsSession() = runBlocking {
        val store = FakeSessionStore()
        val api = FakeRemoteApi()
        val repository = repo(store, api)
        repository.signIn("a@example.com", "password123")
        assertEquals("access-A", repository.restoreSession()?.accessToken)
        assertEquals(0, api.refreshCalls)
    }

    @Test
    fun restoreSessionRefreshesExpiredToken() = runBlocking {
        val store = FakeSessionStore()
        val api = FakeRemoteApi()
        val repository = repo(store, api)
        repository.signIn("a@example.com", "password123")
        // Simulate time passing beyond expiry.
        val later = NazeVerAuthRepository(api, store, { now + 7200_000L })
        val restored = later.restoreSession()
        assertNotNull(restored)
        assertEquals("access-B", restored?.accessToken)
        assertEquals(1, api.refreshCalls)
    }

    @Test
    fun restoreSessionWithRevokedRefreshTokenClearsStore() = runBlocking {
        val store = FakeSessionStore()
        val api = FakeRemoteApi()
        repo(store, api).signIn("a@example.com", "password123")
        api.failRefreshWith = AuthHttpException(401)
        val later = NazeVerAuthRepository(api, store, { now + 7200_000L })
        assertNull(later.restoreSession())
        assertFalse(store.hasSession())
    }

    @Test
    fun failureMessagesNeverContainTokens() = runBlocking {
        val api = FakeRemoteApi().apply { failSignInWith = AuthHttpException(401) }
        val result = repo(FakeSessionStore(), api).signIn("secret-token@example.com", "secret-token-value")
        assertTrue(result is AuthResult.Failure)
        val message = (result as AuthResult.Failure).error.userMessage
        assertFalse(message.contains("secret-token-value"))
        assertFalse(message.contains("secret-token@example.com"))
    }

    @Test
    fun transientRefreshFailureKeepsSession() = runBlocking {
        val store = FakeSessionStore()
        val api = FakeRemoteApi()
        repo(store, api).signIn("a@example.com", "password123")
        api.failRefreshWith = AuthHttpException(500)
        val later = NazeVerAuthRepository(api, store, { now + 7200_000L })
        assertEquals("access-A", later.restoreSession()?.accessToken)
        assertTrue(store.hasSession())
    }
}
