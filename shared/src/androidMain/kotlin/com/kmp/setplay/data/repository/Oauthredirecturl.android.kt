package com.kmp.setplay.data.repository

// androidMain actual
// Matches the intent-filter scheme/host in AndroidManifest.xml.
// Also add "setplay://login-callback" in Supabase Dashboard →
// Auth → URL Configuration → Redirect URLs.
actual fun getOAuthRedirectUrl(): String = "setplay://login-callback"