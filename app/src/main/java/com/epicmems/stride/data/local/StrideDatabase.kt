package com.epicmems.stride.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [StepDay::class, StepHour::class, RouteEntity::class, RoutePointEntity::class],
    version = 2,
    exportSchema = false
)
abstract class StrideDatabase : RoomDatabase() {

    abstract fun stepDao(): StepDao

    abstract fun routeDao(): RouteDao

    companion object {
        /**
         * v2 added `routes.estimated` for paths reconstructed from motion sensors indoors.
         * A real migration (rather than a wipe) so an app update never loses your history.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routes ADD COLUMN estimated INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var instance: StrideDatabase? = null

        fun get(context: Context): StrideDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StrideDatabase::class.java,
                    "stride.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
