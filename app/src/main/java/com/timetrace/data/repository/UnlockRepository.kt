package com.timetrace.data.repository

import com.timetrace.data.local.dao.UnlockRecordDao
import com.timetrace.data.local.entity.UnlockRecordEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnlockRepository @Inject constructor(
    private val unlockRecordDao: UnlockRecordDao
) {

    fun getUnlockRecordsByDate(date: String): Flow<List<UnlockRecordEntity>> {
        return unlockRecordDao.getUnlockRecordsByDate(date)
    }

    fun getUnlockCountByDate(date: String): Flow<Int> {
        return unlockRecordDao.getUnlockCountByDate(date)
    }

    fun getUnlockCountBetweenDates(startDate: String, endDate: String): Flow<Int> {
        return unlockRecordDao.getUnlockCountBetweenDates(startDate, endDate)
    }

    suspend fun insertRecord(record: UnlockRecordEntity) {
        unlockRecordDao.insertRecord(record)
    }

    suspend fun insertRecords(records: List<UnlockRecordEntity>) {
        unlockRecordDao.insertRecords(records)
    }
}
