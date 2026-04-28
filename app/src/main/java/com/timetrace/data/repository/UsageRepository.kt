package com.timetrace.data.repository

import com.timetrace.data.local.dao.DailyTotal
import com.timetrace.data.local.dao.PackageDuration
import com.timetrace.data.local.dao.UsageRecordDao
import com.timetrace.data.local.entity.UsageRecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    private val usageRecordDao: UsageRecordDao
) {

    fun getUsageRecordsByDate(date: String): Flow<List<UsageRecordEntity>> {
        return usageRecordDao.getUsageRecordsByDate(date)
    }

    fun getUsageRecordsBetweenDates(startDate: String, endDate: String): Flow<List<UsageRecordEntity>> {
        return usageRecordDao.getUsageRecordsBetweenDates(startDate, endDate)
    }

    fun getUsageRecordsByPackage(packageName: String): Flow<List<UsageRecordEntity>> {
        return usageRecordDao.getUsageRecordsByPackage(packageName)
    }

    fun getDailyUsageSummary(date: String): Flow<List<PackageDuration>> {
        return usageRecordDao.getDailyUsageSummary(date)
    }

    fun getWeeklyUsageSummary(startDate: String, endDate: String): Flow<List<PackageDuration>> {
        return usageRecordDao.getWeeklyUsageSummary(startDate, endDate)
    }

    fun getTopApps(date: String, limit: Int = 10): Flow<List<PackageDuration>> {
        return usageRecordDao.getTopApps(date, limit)
    }

    fun getDailyTotals(startDate: String, endDate: String): Flow<List<DailyTotal>> {
        return usageRecordDao.getDailyTotals(startDate, endDate)
    }

    fun getPackageDailyTotals(packageName: String, startDate: String, endDate: String): Flow<List<DailyTotal>> {
        return usageRecordDao.getPackageDailyTotals(packageName, startDate, endDate)
    }

    fun getTotalUsageTimeByDate(date: String): Flow<Long?> {
        return usageRecordDao.getTotalUsageTimeByDate(date)
    }

    suspend fun insertRecord(record: UsageRecordEntity): Long {
        return usageRecordDao.insertRecord(record)
    }

    suspend fun insertRecords(records: List<UsageRecordEntity>) {
        usageRecordDao.insertRecords(records)
    }

    suspend fun getRecordByPackageAndStartTime(packageName: String, startTime: Long): UsageRecordEntity? {
        return usageRecordDao.getRecordByPackageAndStartTime(packageName, startTime)
    }

    suspend fun upsertRecord(record: UsageRecordEntity): Long {
        val existing = usageRecordDao.getRecordByPackageAndStartTime(record.packageName, record.startTime)
        return if (existing != null) {
            existing.id
        } else {
            usageRecordDao.insertRecord(record)
        }
    }
}
