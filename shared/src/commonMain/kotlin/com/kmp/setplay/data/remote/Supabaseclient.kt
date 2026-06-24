package com.kmp.setplay.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions


expect fun provideSupabaseUrl(): String
expect fun provideSupabaseKey(): String

fun createSetPlaySupabaseClient(): SupabaseClient = createSupabaseClient(
    supabaseUrl = provideSupabaseUrl(),
    supabaseKey = provideSupabaseKey()
) {
    install(Auth) {
        alwaysAutoRefresh = true
        autoLoadFromStorage = true
    }
    install(Postgrest)
    install(Realtime)
    install(Storage)
    install(Functions)
}