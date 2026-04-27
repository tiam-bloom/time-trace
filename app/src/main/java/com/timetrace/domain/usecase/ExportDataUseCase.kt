package com.timetrace.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.timetrace.data.local.dao.AppInfoDao
import com.timetrace.data.local.dao.ClickRecordDao
import com.timetrace.data.local.dao.UnlockRecordDao
import com.timetrace.data.local.dao.UsageRecordDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageRecordDao: UsageRecordDao,
    private val clickRecordDao: ClickRecordDao,
    private val unlockRecordDao: UnlockRecordDao,
    private val appInfoDao: AppInfoDao
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun exportToCsv(startDate: String, endDate: String): File? = withContext(Dispatchers.IO) {
        try {
            val usageRecords = usageRecordDao.getUsageRecordsBetweenDates(startDate, endDate).first()
            val clickRecords = clickRecordDao.getWeeklyClickSummary(startDate, endDate).first()
            val apps = appInfoDao.getAllAppsOnce()

            val fileName = "timetrace_export_${startDate}_${endDate}.csv"
            val file = File(context.cacheDir, fileName)

            FileWriter(file).use { writer ->
                writer.append("date,app_name,package,usage_hours,click_count\n")

                val clickMap = clickRecords.associate { it.packageName to it.clickCount }
                val appNameMap = apps.associate { it.packageName to it.appName }

                usageRecords.groupBy { it.date }.forEach { (date, records) ->
                    records.groupBy { it.packageName }.forEach { (packageName, packageRecords) ->
                        val totalUsage = packageRecords.sumOf { it.duration }
                        val usageHours = "%.2f".format(totalUsage / 3600000.0)
                        val clicks = clickMap[packageName] ?: 0
                        val appName = appNameMap[packageName] ?: packageName
                        writer.append("$date,$appName,$packageName,$usageHours,$clicks\n")
                    }
                }
            }

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun exportToJson(startDate: String, endDate: String): File? = withContext(Dispatchers.IO) {
        try {
            val usageRecords = usageRecordDao.getUsageRecordsBetweenDates(startDate, endDate).first()
            val clickRecords = clickRecordDao.getDailyClickSummary(startDate).first()
            val unlockRecords = unlockRecordDao.getUnlockRecordsByDate(startDate).first()
            val apps = appInfoDao.getAllApps().first()

            val json = JSONObject()
            json.put("exportDate", dateFormat.format(Date()))
            json.put("startDate", startDate)
            json.put("endDate", endDate)

            val appsJson = JSONArray()
            apps.forEach { app ->
                val appJson = JSONObject()
                appJson.put("packageName", app.packageName)
                appJson.put("appName", app.appName)
                appJson.put("isUninstalled", app.isUninstalled)
                appsJson.put(appJson)
            }
            json.put("apps", appsJson)

            val usageJson = JSONArray()
            usageRecords.forEach { record ->
                val recordJson = JSONObject()
                recordJson.put("packageName", record.packageName)
                recordJson.put("startTime", record.startTime)
                recordJson.put("endTime", record.endTime)
                recordJson.put("duration", record.duration)
                recordJson.put("date", record.date)
                usageJson.put(recordJson)
            }
            json.put("usageRecords", usageJson)

            val clickJson = JSONArray()
            clickRecords.forEach { click ->
                val clickObj = JSONObject()
                clickObj.put("packageName", click.packageName)
                clickObj.put("clickCount", click.clickCount)
                clickJson.put(clickObj)
            }
            json.put("clickSummary", clickJson)

            json.put("totalUnlocks", unlockRecords.size)

            val fileName = "timetrace_export_${startDate}_${endDate}.json"
            val file = File(context.cacheDir, fileName)
            file.writeText(json.toString(2))

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.extension == "csv") "text/csv" else "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "分享数据").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
