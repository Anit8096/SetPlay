package com.kmp.setplay.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions

expect fun provideSupabaseUrl(): String
expect fun provideSupabaseKey(): String

expect fun provideGoogleWebClientId(): String


private val setPlaySupabaseClient: SupabaseClient by lazy {
    createSupabaseClient(
        supabaseUrl = provideSupabaseUrl(),
        supabaseKey = provideSupabaseKey()
    ) {
        install(Auth) {
            alwaysAutoRefresh = true
            autoLoadFromStorage = true
            sessionManager = SettingsSessionManager()
            scheme = "setplay"
            host = "login-callback"
        }
        install(Postgrest)
        install(Realtime)
        install(Storage)
        install(Functions)
    }
}

fun createSetPlaySupabaseClient(): SupabaseClient = setPlaySupabaseClient