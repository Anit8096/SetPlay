package com.kmp.setplay.data.local.db

import androidx.room3.RoomDatabase

/**
 * Platform-specific Room database builder.
 * Android actual uses BundledSQLiteDriver.
 * Web actual (webMain) uses WebWorkerSQLiteDriver via OPFS.
 */
expect fun getDatabaseBuilder(): RoomDatabase.Builder<SetPlayDatabase>