package com.naze.nazever.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.naze.nazever.core.network.auth.NazeVerAuthRepository
import com.naze.nazever.core.network.auth.SupabaseAuthRemoteApi
import com.naze.nazever.core.network.session.SupabaseSessionRemoteApi
import com.naze.nazever.core.security.SecureSessionStore

/**
 * Builds the production auth stack from TASK-005/007 components:
 * Ktor Supabase client + EncryptedSharedPreferences session store +
 * server-side session management (register/refresh/revoke).
 */
class AuthViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            "Unknown ViewModel: " + modelClass.name
        }
        val store = SecureSessionStore(appContext)
        val api = SupabaseAuthRemoteApi.fromBuildConfig()
        val sessionApi = SupabaseSessionRemoteApi.fromBuildConfig()
        val repository = NazeVerAuthRepository(
            remoteApi = api,
            sessionStore = store,
            sessionApi = sessionApi,
            deviceNameProvider = { android.os.Build.MODEL }
        )
        return AuthViewModel(SupabaseAuthGateway(repository)) as T
    }
}
