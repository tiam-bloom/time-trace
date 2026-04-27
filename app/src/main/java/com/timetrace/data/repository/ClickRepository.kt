package com.timetrace.data.repository

import com.timetrace.data.local.dao.ClickRecordDao
import com.timetrace.data.local.dao.PackageClickCount
import com.timetrace.data.local.entity.ClickRecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClickRepository @Inject constructor(
    private val clickRecordDao: ClickRecordDao
) {

    fun getClickRecordsByDate(date: String): Flow<List<ClickRecordEntity>> {
        return clickRecordDao.getClickRecordsByDate(date)
    }

    fun getClickRecordsByPackage(packageName: String): Flow<List<ClickRecordEntity>> {
        return clickRecordDao.getClickRecordsByPackage(packageName)
    }

    fun getDailyClickSummary(date: String): Flow<List<PackageClickCount>> {
        return clickRecordDao.getDailyClickSummary(date)
    }

    fun getWeeklyClickSummary(startDate: String, endDate: String): Flow<List<PackageClickCount>> {
        return clickRecordDao.getWeeklyClickSummary(startDate, endDate)
    }

    fun getTotalClicksByDate(date: String): Flow<Int> {
        return clickRecordDao.getTotalClicksByDate(date)
    }

    suspend fun insertRecord(record: ClickRecordEntity) {
        clickRecordDao.insertRecord(record)
    }

    suspend fun insertRecords(records: List<ClickRecordEntity>) {
        clickRecordDao.insertRecords(records)
    }
}
