package com.epicmems.stride.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    @Query("SELECT * FROM step_days WHERE date = :date LIMIT 1")
    suspend fun day(date: String): StepDay?

    @Query("SELECT * FROM step_days WHERE date = :date LIMIT 1")
    fun observeDay(date: String): Flow<StepDay?>

    @Query("SELECT * FROM step_days ORDER BY date ASC")
    fun observeAll(): Flow<List<StepDay>>

    @Query("SELECT * FROM step_days ORDER BY date ASC")
    suspend fun all(): List<StepDay>

    @Query("SELECT * FROM step_days WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun daysBetween(from: String, to: String): List<StepDay>

    @Upsert
    suspend fun upsert(day: StepDay)

    @Upsert
    suspend fun upsertAll(days: List<StepDay>)

    @Query("UPDATE step_days SET marked = :marked WHERE date = :date")
    suspend fun setMarked(date: String, marked: Boolean)

    @Query("SELECT * FROM step_hours WHERE date = :date ORDER BY hour ASC")
    suspend fun hours(date: String): List<StepHour>

    @Query("SELECT * FROM step_hours WHERE date = :date ORDER BY hour ASC")
    fun observeHours(date: String): Flow<List<StepHour>>

    @Upsert
    suspend fun upsertHour(hour: StepHour)

    @Upsert
    suspend fun upsertHours(hours: List<StepHour>)

    @Query("SELECT COUNT(*) FROM step_days")
    suspend fun count(): Int

    @Query("DELETE FROM step_hours")
    suspend fun clearHours()

    @Query("DELETE FROM step_days")
    suspend fun clearDays()
}
