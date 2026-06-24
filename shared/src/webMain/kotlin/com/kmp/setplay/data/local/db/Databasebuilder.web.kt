package com.kmp.setplay.data.local.db

import androidx.room3.Room
import androidx.room3.RoomDatabase

actual fun getDatabaseBuilder(): RoomDatabase.Builder<SetPlayDatabase> {
    return Room.inMemoryDatabaseBuilder<SetPlayDatabase>()
}