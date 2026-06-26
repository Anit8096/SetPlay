package com.kmp.setplay.di

import com.kmp.setplay.data.local.LocalCache
import io.github.jan.supabase.SupabaseClient

/**
 * Platform-specific LocalCache provider.
 * Android → RoomLocalCache
 * Web     → NoOpLocalCache
 *
 * SupabaseClient passed as parameter in case a future platform
 * needs it for cache invalidation logic.
 */
expect fun provideLocalCache(supabase: SupabaseClient): LocalCache
