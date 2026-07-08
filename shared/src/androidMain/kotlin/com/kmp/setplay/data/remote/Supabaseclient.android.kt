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

/**
 * Called from MainActivity.onNewIntent().
 * handleDeeplinks() extracts OAuth tokens from the intent URI and
 * imports them into the Supabase auth session.
 */
fun handleOAuthRedirect(supabase: SupabaseClient, intent: Intent) {
    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            supabase.handleDeeplinks(intent = intent)
        }
    }
}