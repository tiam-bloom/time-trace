package com.timetrace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.timetrace.data.local.entity.ClickRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClickRecordDao {

    @Query("SELECT * FROM click_record WHERE date = :date ORDER BY timestamp DESC")
    fun getClickRecordsByDate(date: String): Flow<List<ClickRecordEntity>>

    @Query("SELECT * FROM click_record WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getClickRecordsByPackage(packageName: String): Flow<List<ClickRecordEntity>>

    @Query("""
        SELECT packageName, COUNT(*) as clickCount
        FROM click_record
        WHERE date = :date
        GROUP BY packageName
        ORDER BY clickCount DESC
    """)
    fun getDailyClickSummary(date: String): Flow<List<PackageClickCount>>

    @Query("""
        SELECT packageName, COUNT(*) as clickCount
        FROM click_record
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY packageName
        ORDER BY clickCount DESC
    """)
    fun getWeeklyClickSummary(startDate: String, endDate: String): Flow<List<PackageClickCount>>

    @Query("SELECT COUNT(*) FROM click_record WHERE date = :date")
    fun getTotalClicksByDate(date: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ClickRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<ClickRecordEntity>)

    @Query("DELETE FROM click_record")
    suspend fun deleteAll()
}

data class PackageClickCount(
    val packageName: String,
    val clickCount: Int
)
