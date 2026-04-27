package com.timetrace.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.timetrace.data.local.dao.AppInfoDao
import com.timetrace.data.local.dao.ClickRecordDao
import com.timetrace.data.local.dao.UnlockRecordDao
import com.timetrace.data.local.dao.UsageRecordDao
import com.timetrace.data.local.entity.AppInfoEntity
import com.timetrace.data.local.entity.ClickRecordEntity
import com.timetrace.data.local.entity.UnlockRecordEntity
import com.timetrace.data.local.entity.UsageRecordEntity

@Database(
    entities = [
        AppInfoEntity::class,
        UsageRecordEntity::class,
        ClickRecordEntity::class,
        UnlockRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TimeTraceDatabase : RoomDatabase() {
    abstract fun appInfoDao(): AppInfoDao
    abstract fun usageRecordDao(): UsageRecordDao
    abstract fun clickRecordDao(): ClickRecordDao
    abstract fun unlockRecordDao(): UnlockRecordDao
}
