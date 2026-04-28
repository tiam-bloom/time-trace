package com.timetrace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.timetrace.data.local.entity.UsageRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageRecordDao {

    @Query("SELECT * FROM usage_record WHERE date = :date ORDER BY startTime DESC")
    fun getUsageRecordsByDate(date: String): Flow<List<UsageRecordEntity>>

    @Query("SELECT * FROM usage_record WHERE date BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    fun getUsageRecordsBetweenDates(startDate: String, endDate: String): Flow<List<UsageRecordEntity>>

    @Query("SELECT * FROM usage_record WHERE packageName = :packageName ORDER BY startTime DESC")
    fun getUsageRecordsByPackage(packageName: String): Flow<List<UsageRecordEntity>>

    @Query("""
        SELECT packageName, SUM(duration) as totalDuration
        FROM usage_record
        WHERE date = :date
        GROUP BY packageName
        ORDER BY totalDuration DESC
    """)
    fun getDailyUsageSummary(date: String): Flow<List<PackageDuration>>

    @Query("""
        SELECT packageName, SUM(duration) as totalDuration
        FROM usage_record
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY packageName
        ORDER BY totalDuration DESC
    """)
    fun getWeeklyUsageSummary(startDate: String, endDate: String): Flow<List<PackageDuration>>

    @Query("""
        SELECT packageName, SUM(duration) as totalDuration
        FROM usage_record
        WHERE date = :date
        GROUP BY packageName
        ORDER BY totalDuration DESC
        LIMIT :limit
    """)
    fun getTopApps(date: String, limit: Int): Flow<List<PackageDuration>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecord(record: UsageRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecords(records: List<UsageRecordEntity>)

    @Query("SELECT * FROM usage_record WHERE packageName = :packageName AND startTime = :startTime")
    suspend fun getRecordByPackageAndStartTime(packageName: String, startTime: Long): UsageRecordEntity?

    @Query("UPDATE usage_record SET endTime = :endTime, duration = :duration, isCompleted = :isCompleted WHERE packageName = :packageName AND startTime = :startTime")
    suspend fun updateRecord(packageName: String, startTime: Long, endTime: Long, duration: Long, isCompleted: Boolean): Int

    @Query("SELECT SUM(duration) FROM usage_record WHERE date = :date")
    fun getTotalUsageTimeByDate(date: String): Flow<Long?>

    @Query("""
        SELECT date, SUM(duration) as totalDuration
        FROM usage_record
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getDailyTotals(startDate: String, endDate: String): Flow<List<DailyTotal>>

    @Query("""
        SELECT date, SUM(duration) as totalDuration
        FROM usage_record
        WHERE packageName = :packageName AND date BETWEEN :startDate AND :endDate
        GROUP BY date
        ORDER BY date ASC
    """)
    fun getPackageDailyTotals(packageName: String, startDate: String, endDate: String): Flow<List<DailyTotal>>

    @Query("""
        SELECT packageName, COUNT(*) as launchCount
        FROM usage_record
        WHERE date = :date
        GROUP BY packageName
        ORDER BY launchCount DESC
    """)
    fun getDailyLaunchSummary(date: String): Flow<List<PackageLaunchCount>>

    @Query("""
        SELECT packageName, COUNT(*) as launchCount
        FROM usage_record
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY packageName
        ORDER BY launchCount DESC
    """)
    fun getWeeklyLaunchSummary(startDate: String, endDate: String): Flow<List<PackageLaunchCount>>

    @Query("SELECT COUNT(*) FROM usage_record WHERE date = :date")
    fun getTotalLaunchesByDate(date: String): Flow<Int>

    @Query("DELETE FROM usage_record")
    suspend fun deleteAll()
}

data class PackageDuration(
    val packageName: String,
    val totalDuration: Long
)

data class DailyTotal(
    val date: String,
    val totalDuration: Long
)

data class PackageLaunchCount(
    val packageName: String,
    val launchCount: Int
)
