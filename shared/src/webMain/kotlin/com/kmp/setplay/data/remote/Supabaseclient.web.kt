package com.kmp.setplay.data.remote

import com.kmp.setplay.BuildKonfig
import com.kmp.setplay.data.remote.createSetPlaySupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.parseFragmentAndImportSession

actual fun provideSupabaseUrl(): String = BuildKonfig.SUPABASE_URL
actual fun provideSupabaseKey(): String = BuildKonfig.SUPABASE_ANON_KEY
actual suspend fun parseSessionFromUrl() {
    runCatching {
    }
}