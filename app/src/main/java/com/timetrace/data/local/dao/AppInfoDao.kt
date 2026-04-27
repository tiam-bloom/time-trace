package com.timetrace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.timetrace.data.local.entity.AppInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {

    @Query("SELECT * FROM app_info ORDER BY lastSeenTime DESC")
    fun getAllApps(): Flow<List<AppInfoEntity>>

    @Query("SELECT * FROM app_info ORDER BY lastSeenTime DESC")
    suspend fun getAllAppsOnce(): List<AppInfoEntity>

    @Query("SELECT * FROM app_info WHERE packageName = :packageName")
    suspend fun getAppByPackage(packageName: String): AppInfoEntity?

    @Query("SELECT * FROM app_info WHERE isUninstalled = 1")
    fun getUninstalledApps(): Flow<List<AppInfoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppInfoEntity>)

    @Update
    suspend fun updateApp(app: AppInfoEntity)

    @Query("UPDATE app_info SET isUninstalled = 1, lastSeenTime = :timestamp WHERE packageName = :packageName")
    suspend fun markAsUninstalled(packageName: String, timestamp: Long)

    @Query("UPDATE app_info SET lastSeenTime = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastSeenTime(packageName: String, timestamp: Long)

    @Query("DELETE FROM app_info")
    suspend fun deleteAll()
}
