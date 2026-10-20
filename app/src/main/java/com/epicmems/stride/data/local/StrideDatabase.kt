package com.epicmems.stride.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StepDay::class, StepHour::class, RouteEntity::class, RoutePointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StrideDatabase : RoomDatabase() {

    abstract fun stepDao(): StepDao

    abstract fun routeDao(): RouteDao

    companion object {
        @Volatile
        private var instance: StrideDatabase? = null

        fun get(context: Context): StrideDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StrideDatabase::class.java,
                    "stride.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
