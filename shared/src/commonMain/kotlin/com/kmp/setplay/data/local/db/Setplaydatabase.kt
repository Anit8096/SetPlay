package com.kmp.setplay.data.local.db

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.kmp.setplay.data.local.dao.AnnouncementDao
import com.kmp.setplay.data.local.dao.DeviceTokenDao
import com.kmp.setplay.data.local.dao.MatchDao
import com.kmp.setplay.data.local.dao.PlayerDao
import com.kmp.setplay.data.local.dao.RoundDao
import com.kmp.setplay.data.local.dao.StandingDao
import com.kmp.setplay.data.local.dao.TeamDao
import com.kmp.setplay.data.local.dao.TournamentDao
import com.kmp.setplay.data.local.entity.AnnouncementEntity
import com.kmp.setplay.data.local.entity.DeviceTokenEntity
import com.kmp.setplay.data.local.entity.MatchEntity
import com.kmp.setplay.data.local.entity.PlayerEntity
import com.kmp.setplay.data.local.entity.RoundEntity
import com.kmp.setplay.data.local.entity.StandingEntity
import com.kmp.setplay.data.local.entity.TeamEntity
import com.kmp.setplay.data.local.entity.TournamentEntity

/**
 * Room 3.0 KMP database.
 *
 * Rules upheld:
 * - @ConstructedBy with expect object implementing RoomDatabaseConstructor (rule 6).
 * - getDatabaseBuilder() is expect/actual — Android uses BundledSQLiteDriver,
 *   Web uses WebWorkerSQLiteDriver via OPFS (rule 5).
 * - Schema directory declared in shared/build.gradle.kts (rule 6).
 */
@Database(
    entities = [
        TournamentEntity::class,
        TeamEntity::class,
        PlayerEntity::class,
        RoundEntity::class,
        MatchEntity::class,
        StandingEntity::class,
        AnnouncementEntity::class,
        DeviceTokenEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(SetPlayDatabaseConstructor::class)
abstract class SetPlayDatabase : RoomDatabase() {
    abstract fun tournamentDao(): TournamentDao
    abstract fun teamDao(): TeamDao
    abstract fun playerDao(): PlayerDao
    abstract fun roundDao(): RoundDao
    abstract fun matchDao(): MatchDao
    abstract fun standingDao(): StandingDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun deviceTokenDao(): DeviceTokenDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object SetPlayDatabaseConstructor : RoomDatabaseConstructor<SetPlayDatabase> {
    override fun initialize(): SetPlayDatabase
}