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
                eventBuilder.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND ||
                eventBuilder.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                eventBuilder.eventType == UsageEvents.Event.ACTIVITY_PAUSED
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
        var foregroundTime: Long = 0

        for (event in sortedEvents) {
            val packageName = event.packageName
            if (packageName.isEmpty()) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (currentPackage != null && foregroundTime > 0) {
                        val duration = event.timeMillis - foregroundTime
                        if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
                            saveUsageRecord(currentPackage, foregroundTime, event.timeMillis, duration)
                        }
                    }
                    currentPackage = packageName
                    foregroundTime = event.timeMillis
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (packageName == currentPackage && foregroundTime > 0) {
                        val duration = event.timeMillis - foregroundTime
                        if (duration > 0 && duration < 24 * 60 * 60 * 1000) {
                            saveUsageRecord(packageName, foregroundTime, event.timeMillis, duration)
                        }
                        currentPackage = null
                        foregroundTime = 0
                    }
                }
            }
        }

        if (currentPackage != null && foregroundTime > 0) {
            val duration = System.currentTimeMillis() - foregroundTime
            if (duration > 0 && duration < 60 * 60 * 1000) {
                saveUsageRecord(currentPackage, foregroundTime, System.currentTimeMillis(), duration)
            }
        }
    }

    private suspend fun saveUsageRecord(packageName: String, startTime: Long, endTime: Long, duration: Long) {
        try {
            val appName = getAppName(packageName)
            val date = dateFormat.format(Date(startTime))

            appRepository.getOrCreateApp(packageName, appName, startTime)

            val record = UsageRecordEntity(
                packageName = packageName,
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                date = date
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
