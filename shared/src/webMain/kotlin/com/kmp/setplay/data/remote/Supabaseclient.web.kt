package com.kmp.setplay.data.remote

import com.kmp.setplay.BuildKonfig
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.parseFragmentAndImportSession
import kotlinx.browser.window

actual fun provideSupabaseUrl(): String = BuildKonfig.SUPABASE_URL
actual fun provideSupabaseKey(): String = BuildKonfig.SUPABASE_ANON_KEY

@OptIn(SupabaseInternal::class)
actual suspend fun parseSessionFromUrl() {
    // window.location.hash gives the URL fragment including the '#' prefix.
    // e.g. "#access_token=...&refresh_token=..."
    // We strip the leading '#' before passing to parseFragmentAndImportSession.
    val fragment = window.location.hash.removePrefix("#")
    if (fragment.isNotBlank()) {
        runCatching {
            createSetPlaySupabaseClient().auth.parseFragmentAndImportSession(fragment)
        }
    }
}