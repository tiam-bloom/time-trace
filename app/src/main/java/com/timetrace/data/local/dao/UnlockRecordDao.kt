package com.timetrace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.timetrace.data.local.entity.UnlockRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockRecordDao {

    @Query("SELECT * FROM unlock_record WHERE date = :date ORDER BY timestamp DESC")
    fun getUnlockRecordsByDate(date: String): Flow<List<UnlockRecordEntity>>

    @Query("SELECT COUNT(*) FROM unlock_record WHERE date = :date")
    fun getUnlockCountByDate(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM unlock_record WHERE date BETWEEN :startDate AND :endDate")
    fun getUnlockCountBetweenDates(startDate: String, endDate: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: UnlockRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<UnlockRecordEntity>)
}
