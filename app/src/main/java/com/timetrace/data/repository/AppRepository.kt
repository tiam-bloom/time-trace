package com.timetrace.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.timetrace.data.local.dao.AppInfoDao
import com.timetrace.data.local.entity.AppInfoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val appInfoDao: AppInfoDao,
    @ApplicationContext private val context: Context
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

    suspend fun updateAppName(packageName: String, appName: String) {
        val existing = appInfoDao.getAppByPackage(packageName)
        if (existing != null && existing.appName != appName) {
            appInfoDao.updateApp(existing.copy(appName = appName))
        }
    }

    suspend fun refreshAllAppNames() {
        val apps = appInfoDao.getAllAppsOnce()
        apps.forEach { app ->
            val freshName = getAppNameFromPackageManager(app.packageName)
            if (freshName != app.packageName && freshName.isNotBlank()) {
                appInfoDao.updateApp(app.copy(appName = freshName))
            }
        }
    }

    private fun getAppNameFromPackageManager(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    suspend fun getOrCreateApp(packageName: String, appName: String, timestamp: Long): AppInfoEntity {
        val existing = appInfoDao.getAppByPackage(packageName)
        val validAppName = appName.isNotBlank() && appName != packageName

        return if (existing != null) {
            val shouldUpdate = (validAppName && existing.appName != appName) || existing.isUninstalled

            if (shouldUpdate) {
                appInfoDao.updateApp(existing.copy(
                    appName = if (validAppName) appName else existing.appName,
                    lastSeenTime = timestamp,
                    isUninstalled = false
                ))
            } else {
                appInfoDao.updateLastSeenTime(packageName, timestamp)
            }
            existing.copy(
                appName = if (validAppName) appName else existing.appName,
                lastSeenTime = timestamp,
                isUninstalled = false
            )
        } else {
            val newApp = AppInfoEntity(
                packageName = packageName,
                appName = if (validAppName) appName else packageName,
                firstSeenTime = timestamp,
                lastSeenTime = timestamp,
                isUninstalled = false
            )
            appInfoDao.insertApp(newApp)
            newApp
        }
    }
}
