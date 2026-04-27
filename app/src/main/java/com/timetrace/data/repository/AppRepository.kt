package com.timetrace.data.repository

import com.timetrace.data.local.dao.AppInfoDao
import com.timetrace.data.local.entity.AppInfoEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val appInfoDao: AppInfoDao
) {

    fun getAllApps(): Flow<List<AppInfoEntity>> = appInfoDao.getAllApps()

    fun getUninstalledApps(): Flow<List<AppInfoEntity>> = appInfoDao.getUninstalledApps()

    suspend fun getAppByPackage(packageName: String): AppInfoEntity? {
        return appInfoDao.getAppByPackage(packageName)
    }

    suspend fun insertApp(app: AppInfoEntity) {
        appInfoDao.insertApp(app)
    }

    suspend fun insertApps(apps: List<AppInfoEntity>) {
        appInfoDao.insertApps(apps)
    }

    suspend fun markAsUninstalled(packageName: String, timestamp: Long) {
        appInfoDao.markAsUninstalled(packageName, timestamp)
    }

    suspend fun updateLastSeenTime(packageName: String, timestamp: Long) {
        appInfoDao.updateLastSeenTime(packageName, timestamp)
    }

    suspend fun getOrCreateApp(packageName: String, appName: String, timestamp: Long): AppInfoEntity {
        val existing = appInfoDao.getAppByPackage(packageName)
        return if (existing != null) {
            if (existing.isUninstalled) {
                appInfoDao.updateApp(existing.copy(isUninstalled = false, lastSeenTime = timestamp))
            } else {
                appInfoDao.updateLastSeenTime(packageName, timestamp)
            }
            existing.copy(lastSeenTime = timestamp, isUninstalled = false)
        } else {
            val newApp = AppInfoEntity(
                packageName = packageName,
                appName = appName,
                firstSeenTime = timestamp,
                lastSeenTime = timestamp,
                isUninstalled = false
            )
            appInfoDao.insertApp(newApp)
            newApp
        }
    }
}
