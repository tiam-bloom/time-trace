package com.timetrace.di

import android.content.Context
import androidx.room.Room
import com.timetrace.data.local.dao.AppInfoDao
import com.timetrace.data.local.dao.ClickRecordDao
import com.timetrace.data.local.dao.UnlockRecordDao
import com.timetrace.data.local.dao.UsageRecordDao
import com.timetrace.data.local.db.TimeTraceDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TimeTraceDatabase {
        return Room.databaseBuilder(
            context,
            TimeTraceDatabase::class.java,
            "timetrace_database"
        ).build()
    }

    @Provides
    fun provideAppInfoDao(database: TimeTraceDatabase): AppInfoDao {
        return database.appInfoDao()
    }

    @Provides
    fun provideUsageRecordDao(database: TimeTraceDatabase): UsageRecordDao {
        return database.usageRecordDao()
    }

    @Provides
    fun provideClickRecordDao(database: TimeTraceDatabase): ClickRecordDao {
        return database.clickRecordDao()
    }

    @Provides
    fun provideUnlockRecordDao(database: TimeTraceDatabase): UnlockRecordDao {
        return database.unlockRecordDao()
    }
}
