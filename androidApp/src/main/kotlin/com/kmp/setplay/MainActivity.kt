package com.kmp.setplay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kmp.setplay.data.local.db.appContext
import com.kmp.setplay.data.remote.handleOAuthRedirect
import com.kmp.setplay.data.remote.createSetPlaySupabaseClient

class MainActivity : ComponentActivity() {

    // Create a single supabase instance for use in onNewIntent.
    // This is the same instance Koin provides — we grab it directly
    // since Koin isn't available at Activity level (it lives in KoinApplication).
    private val supabase by lazy { createSetPlaySupabaseClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        appContext = this.applicationContext
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // handleOAuthRedirect is defined in androidMain/SupabaseClient.android.kt
        // It calls supabase.auth.handleDeeplinks(intent) to import the session.
        if (intent.data != null) {
            handleOAuthRedirect(supabase, intent)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}