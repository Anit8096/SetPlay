package com.kmp.setplay.data.local.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

lateinit var appContext: Context

fun getDatabaseBuilder(): RoomDatabase.Builder<SetPlayDatabase> {
    val dbFile = appContext.getDatabasePath("setplay.db")
    return Room.databaseBuilder<SetPlayDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    ).setDriver(BundledSQLiteDriver())
}