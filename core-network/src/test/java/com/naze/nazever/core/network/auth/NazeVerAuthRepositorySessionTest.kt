package com.naze.nazever.core.network.auth

import com.naze.nazever.core.network.session.DeviceSessionInfo
import com.naze.nazever.core.network.session.RegisteredSession
import com.naze.nazever.core.network.session.SessionRemoteApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FakeSessionRemoteApi : SessionRemoteApi {
    var registerCalls = 0
    var revokedChecks = 0
    var refreshCalls = 0
    val revokeCalls = mutableListOf<String>()
    var failRegisterWith: Exception? = null
    var failRefreshWith: Exception? = null
    var sessionRevoked = false
    var listResult = listOf(
        DeviceSessionInfo("session-B", "device-B", "Galaxy S24", "android", null, null, null)
    )
    var refreshResponse = AuthTokenResponse(
        accessToken = "access-B",
        refreshToken = "refresh-B",
        expiresIn = 3600,
        user = AuthUser(id = "user-A")
    )

    override suspend fun registerSession(
        accessToken: String,
        deviceName: String,
        platform: String,
        refreshToken: String
    ): RegisteredSession {
        failRegisterWith?.let { throw it }
        registerCalls++
        return RegisteredSession("session-A", "device-A")
    }

    override suspend fun refreshSession(refreshToken: String): AuthTokenResponse {
        failRefreshWith?.let { throw it }
        refreshCalls++
        return refreshResponse
    }

    override suspend fun isSessionRevoked(accessToken: String, sessionId: String): Boolean {
        revokedChecks++
        return sessionRevoked
    }

    override suspend fun listDeviceSessions(accessToken: String): List<DeviceSessionInfo> =
        listResult

    override suspend fun revokeSession(accessToken: String, sessionId: String) {
        revokeCalls.add(sessionId)
    }
}

class NazeVerAuthRepositorySessionTest {

    private val now = 1_000_000L

    private fun repo(
        store: FakeSessionStore,
        api: FakeRemoteApi,
        sessionApi: FakeSessionRemoteApi
    ): NazeVerAuthRepository = NazeVerAuthRepository(api, store, { now }, sessionApi)

    @Test
    fun signInRegistersDeviceSession() = runBlocking {
        val store = FakeSessionStore()
        val sessionApi = FakeSessionRemoteApi()
        val result = repo(store, FakeRemoteApi(), sessionApi)
            .signIn("a@example.com", "password123")
        assertTrue(result is AuthResult.Success)
        assertEquals(1, sessionApi.registerCalls)
        assertEquals("session-A", store.read()?.sessionId)
        assertEquals("device-A", store.read()?.deviceId)
    }

    @Test
    fun signInWithoutSessionApiStillSucceeds() = runBlocking {
        val store = FakeSessionStore()
        val result = NazeVerAuthRepository(FakeRemoteApi(), store, { now })
            .signIn("a@example.com", "password123")
        assertTrue(result is AuthResult.Success)
        assertNull(store.read()?.sessionId)
    }

    @Test
    fun registerFailureDegradesGracefully() = runBlocking {
        val store = FakeSessionStore()
        val sessionApi = FakeSessionRemoteApi().apply {
            failRegisterWith = java.io.IOException("offline")
        }
        val result = repo(store, FakeRemoteApi(), sessionApi)
            .signIn("a@example.com", "password123")
        assertTrue(result is AuthResult.Success)
        assertNull(store.read()?.sessionId)
    }

    @Test
    fun restoreVerifiesRevocation() = runBlocking {
        val store = FakeSessionStore()
        val sessionApi = FakeSessionRemoteApi()
        val repository = repo(store, FakeRemoteApi(), sessionApi)
        repository.signIn("a@example.com", "password123")
        val restored = repository.restoreSession()
        assertEquals("access-A", restored?.accessToken)
        assertEquals(1, sessionApi.revokedChecks)
    }

    @Test
    fun revokedSessionIsClearedOnRestore() = runBlocking {
        val store = FakeSessionStore()
        val sessionApi = FakeSessionRemoteApi().apply { sessionRevoked = true }
        val repository = repo(store, FakeRemoteApi(), sessionApi)
        repository.signIn("a@example.com", "password123")
        assertNull(repository.restoreSession())
        assertFalse(store.hasSession())
    }

    @Test
    fun refreshGoesThroughChokePoint() = runBlocking {
        val store = FakeSessionStore()
        val sessionApi = FakeSessionRemoteApi()
        val api = FakeRemoteApi()
        repo(store, api, sessionApi).signIn("a@example.com", "password123")
        val later = NazeVerAuthRepository(api, store, { now + 7200_000L }, sessionApi)
        val restored = later.restoreSession()
        assertEquals("access-B", restored?.accessToken)
        assertEquals(1, sessionApi.refreshCalls)
        assertEquals(0, api.refreshCalls)
        assertEquals("session-A", store.read()?.sessionId)
    }

    @Test
    fun revokedRefreshClearsStore() = runBlocking {
        val store = FakeSessionStore()
        val sessionApi = FakeSessionRemoteApi().apply {
            failRefreshWith = AuthHttpException(401)
        }
        val api = FakeRemoteApi()
        repo(store, api, sessionApi).signIn("a@example.com", "password123")
        val later = NazeVerAuthRepository(api, store, { now + 7200_000L }, sessionApi)
        assertNull(later.restoreSession())
        assertFalse(store.hasSession())
    }

    @Test
    fun unregisteredSessionFallsBackToDirectRefresh() = runBlocking {
        val store = FakeSessionStore()
        val sessionApi = FakeSessionRemoteApi().apply {
            failRegisterWith = java.io.IOException("offline")
        }
        val api = FakeRemoteApi()
        repo(store, api, sessionApi).signIn("a@example.com", "password123")
        val later = NazeVerAuthRepository(api, store, { now + 7200_000L }, sessionApi)
        val restored = later.restoreSession()
        assertEquals("access-B", restored?.accessToken)
        assertEquals(1, api.refreshCalls)
        assertEquals(0, sessionApi.refreshCalls)
    }

    @Test
    fun signOutRevokesOwnSessionRow() = runBlocking {
        val store = FakeSessionStore()
        val sessionApi = FakeSessionRemoteApi()
        val api = FakeRemoteApi()
        val repository = repo(store, api, sessionApi)
        repository.signIn("a@example.com", "password123")
        repository.signOut()
        assertTrue(sessionApi.revokeCalls.contains("session-A"))
        assertEquals(1, api.signOutCalls)
        assertFalse(store.hasSession())
    }

    @Test
    fun listDeviceSessionsDelegates() = runBlocking {
        val store = FakeSessionStore()
        val sessionApi = FakeSessionRemoteApi()
        val repository = repo(store, FakeRemoteApi(), sessionApi)
        repository.signIn("a@example.com", "password123")
        val devices = repository.listDeviceSessions()
        assertEquals(1, devices.size)
        assertEquals("session-B", devices[0].sessionId)
    }

    @Test
    fun revokeOtherSessionDelegates() = runBlocking {
        val store = FakeSessionStore()
        val sessionApi = FakeSessionRemoteApi()
        val repository = repo(store, FakeRemoteApi(), sessionApi)
        repository.signIn("a@example.com", "password123")
        repository.revokeDeviceSession("session-B")
        assertTrue(sessionApi.revokeCalls.contains("session-B"))
    }

    @Test
    fun revokingCurrentSessionIsRejected() = runBlocking {
        val store = FakeSessionStore()
        val sessionApi = FakeSessionRemoteApi()
        val repository = repo(store, FakeRemoteApi(), sessionApi)
        repository.signIn("a@example.com", "password123")
        try {
            repository.revokeDeviceSession("session-A")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected: current device must use signOut instead.
        }
        assertFalse(sessionApi.revokeCalls.contains("session-A"))
    }
}
