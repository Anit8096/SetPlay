package com.kmp.setplay.data.remote

import com.kmp.setplay.BuildKonfig

actual fun provideSupabaseUrl(): String = BuildKonfig.SUPABASE_URL
actual fun provideSupabaseKey(): String = BuildKonfig.SUPABASE_ANON_KEY