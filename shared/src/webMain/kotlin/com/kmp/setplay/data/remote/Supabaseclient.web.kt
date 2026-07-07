package com.kmp.setplay.data.remote

import com.kmp.setplay.BuildKonfig

actual fun provideSupabaseUrl(): String = BuildKonfig.SUPABASE_URL
actual fun provideSupabaseKey(): String = BuildKonfig.SUPABASE_ANON_KEY
actual fun provideGoogleWebClientId(): String = BuildKonfig.GOOGLE_WEB_CLIENT_ID