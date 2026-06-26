package com.kmp.setplay.data.remote

import android.content.Intent
import com.kmp.setplay.BuildKonfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

actual fun provideSupabaseUrl(): String = BuildKonfig.SUPABASE_URL
actual fun provideSupabaseKey(): String = BuildKonfig.SUPABASE_ANON_KEY

actual suspend fun parseSessionFromUrl() {
    // No-op on Android — handled in handleOAuthRedirect() below
}

/**
 * Called from MainActivity.onNewIntent().
 * handleDeeplinks() expects an Android Intent, not a raw URL string.
 * We reconstruct a minimal Intent with the URI data so the library
 * can extract the OAuth tokens from it.
 */
fun handleOAuthRedirect(supabase: SupabaseClient, intent: Intent) {
    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            supabase.handleDeeplinks(intent = intent)
        }
    }
}