package com.timetrace.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.timetrace.data.local.entity.UsageRecordEntity
import com.timetrace.data.repository.AppRepository
import com.timetrace.data.repository.UsageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsManager: UsageStatsManager,
    private val usageRepository: UsageRepository,
    private val appRepository: AppRepository
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    data class EventData(
        val packageName: String,
        val eventType: Int,
        val timeMillis: Long
    )

    suspend fun collectUsageStats() = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.HOUR, -24)
        val startTime = calendar.timeInMillis

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val eventBuilder = UsageEvents.Event()
        val eventList = mutableListOf<EventData>()

        while (events.hasNextEvent()) {
            events.getNextEvent(eventBuilder)
            if (eventBuilder.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                eventBuilder.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
            ) {
                eventList.add(
                    EventData(
                        packageName = eventBuilder.packageName ?: "",
                        eventType = eventBuilder.eventType,
                        timeMillis = eventBuilder.timeStamp
                    )
                )
            }
        }

        processEvents(eventList)
    }

    private suspend fun processEvents(events: List<EventData>) {
        if (events.isEmpty()) return

        val sortedEvents = events.sortedBy { it.timeMillis }
        var currentPackage: String? = null
        var foregroundStartTime: Long = 0

        // background事件不立即关闭session，先记录下来
        // 等下一个foreground事件判断：同APP内切页面 vs 真正切换APP
        var pendingBackgroundPackage: String? = null
        var pendingBackgroundTime: Long = 0

        for (event in sortedEvents) {
            val packageName = event.packageName
            if (packageName.isEmpty()) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (packageName == pendingBackgroundPackage
                        && event.timeMillis - pendingBackgroundTime < 5000
                    ) {
                        // 同APP内切页面（5秒内同一APP又回到前台）：恢复session，不创建新记录
                        currentPackage = packageName
                        // foregroundStartTime 保持原值不变
                        pendingBackgroundPackage = null
                        pendingBackgroundTime = 0
                    } else {
                        // 真正切换了APP：关闭旧session，开始新session
                        if (pendingBackgroundPackage != null) {
                            // 有未处理的background事件，关闭旧session
                            if (foregroundStartTime > 0) {
                                val duration = pendingBackgroundTime - foregroundStartTime
                                if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
                                    saveUsageRecord(pendingBackgroundPackage!!, foregroundStartTime, pendingBackgroundTime, duration, isCompleted = true)
                                }
                            }
                            pendingBackgroundPackage = null
                            pendingBackgroundTime = 0
                        } else if (currentPackage != null && foregroundStartTime > 0) {
                            // 事件丢失：没有收到当前应用的background，但新的foreground已到来
                            // 用新事件时间作为近似结束时间，避免session丢失
                            val duration = event.timeMillis - foregroundStartTime
                            if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
                                saveUsageRecord(currentPackage!!, foregroundStartTime, event.timeMillis, duration, isCompleted = true)
                            }
                        }
                        currentPackage = packageName
                        foregroundStartTime = event.timeMillis
                    }
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    if (packageName == currentPackage && foregroundStartTime > 0) {
                        // 暂记background事件，不关闭session
                        pendingBackgroundPackage = packageName
                        pendingBackgroundTime = event.timeMillis
                        currentPackage = null
                    }
                }
            }
        }

        // 处理未关闭的session
        if (pendingBackgroundPackage != null && foregroundStartTime > 0) {
            // 最后一个事件是background，正常关闭session
            val duration = pendingBackgroundTime - foregroundStartTime
            if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
                saveUsageRecord(pendingBackgroundPackage!!, foregroundStartTime, pendingBackgroundTime, duration, isCompleted = true)
            }
        } else if (currentPackage != null && foregroundStartTime > 0) {
            // 最后一个事件是foreground（APP仍在前台），保存为进行中状态
            val now = System.currentTimeMillis()
            val duration = now - foregroundStartTime
            if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
                saveUsageRecord(currentPackage, foregroundStartTime, now, duration, isCompleted = false)
            }
        }
    }

    private suspend fun saveUsageRecord(
        packageName: String,
        startTime: Long,
        endTime: Long,
        duration: Long,
        isCompleted: Boolean
    ) {
        try {
            val appName = getAppName(packageName)
            val date = dateFormat.format(Date(startTime))

            appRepository.getOrCreateApp(packageName, appName, startTime)

            val record = UsageRecordEntity(
                packageName = packageName,
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                date = date,
                isCompleted = isCompleted
            )
            usageRepository.upsertRecord(record)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    fun hasUsageStatsPermission(): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )
        return stats != null && stats.isNotEmpty()
    }
}
