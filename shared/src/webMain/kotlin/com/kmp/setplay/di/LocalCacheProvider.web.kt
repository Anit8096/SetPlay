package com.kmp.setplay.di

import com.kmp.setplay.data.local.LocalCache
import com.kmp.setplay.data.local.NoOpLocalCache
import io.github.jan.supabase.SupabaseClient

actual fun provideLocalCache(supabase: SupabaseClient): LocalCache = NoOpLocalCache()
