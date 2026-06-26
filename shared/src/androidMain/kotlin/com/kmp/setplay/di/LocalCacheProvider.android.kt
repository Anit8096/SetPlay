package com.kmp.setplay.di

import com.kmp.setplay.data.local.LocalCache
import com.kmp.setplay.data.local.RoomLocalCache
import com.kmp.setplay.data.local.db.SetPlayDatabase
import io.github.jan.supabase.SupabaseClient
import org.koin.core.context.GlobalContext

actual fun provideLocalCache(supabase: SupabaseClient): LocalCache {
    val db = GlobalContext.get().get<SetPlayDatabase>()
    return RoomLocalCache(
        tournamentDao = db.tournamentDao(),
        teamDao = db.teamDao(),
        roundDao = db.roundDao(),
        matchDao = db.matchDao(),
        standingDao = db.standingDao(),
        announcementDao = db.announcementDao()
    )
}
