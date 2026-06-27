package com.kmp.setplay.di

import com.kmp.setplay.data.local.LocalCache
import com.kmp.setplay.data.local.RoomLocalCache
import com.kmp.setplay.data.local.db.SetPlayDatabase
import com.kmp.setplay.data.local.db.getDatabaseBuilder
import org.koin.dsl.module

/**
 * Android-only Koin module for Room database.
 * Not included on web — no Room on JS/WasmJS targets.
 *
 * Add this to startKoin in SetPlayApp.kt:
 *   modules(appModule, androidDatabaseModule)
 */
val androidDatabaseModule = module {
    single<SetPlayDatabase> {
        getDatabaseBuilder().build()
    }

    single<LocalCache> {
        val db = get<SetPlayDatabase>()
        RoomLocalCache(
            tournamentDao = db.tournamentDao(),
            teamDao = db.teamDao(),
            roundDao = db.roundDao(),
            matchDao = db.matchDao(),
            standingDao = db.standingDao(),
            announcementDao = db.announcementDao()
        )
    }
}
