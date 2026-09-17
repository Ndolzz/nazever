package com.nazeworks.nazever.di

import android.content.Context
import com.nazeworks.nazever.BuildConfig
import com.nazeworks.nazever.data.auth.AuthRepositoryImpl
import com.nazeworks.nazever.data.pairing.PairingRepositoryImpl
import com.nazeworks.nazever.domain.model.UserId
import com.nazeworks.nazever.domain.repository.AuthRepository
import com.nazeworks.nazever.domain.repository.PairingRepository
import com.nazeworks.nazever.network.SupabaseClientProvider
import com.nazeworks.nazever.security.SecureSessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient =
        SupabaseClientProvider.get(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY
        )

    @Provides
    @Singleton
    fun provideSecureSessionStore(@ApplicationContext context: Context): SecureSessionStore =
        SecureSessionStore(context)

    @Provides
    @Singleton
    fun provideAuthRepository(
        supabase: SupabaseClient,
        secureSessionStore: SecureSessionStore
    ): AuthRepository = AuthRepositoryImpl(supabase, secureSessionStore)

    @Provides
    @Singleton
    fun providePairingRepository(supabase: SupabaseClient): PairingRepository =
        PairingRepositoryImpl(
            supabase = supabase,
            currentUserIdProvider = {
                supabase.auth.currentUserOrNull()?.id?.let { UserId(it) }
            }
        )
}
