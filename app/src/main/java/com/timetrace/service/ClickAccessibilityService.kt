package com.timetrace.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.timetrace.data.local.entity.ClickRecordEntity
import com.timetrace.data.repository.AppRepository
import com.timetrace.data.repository.ClickRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ClickAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var clickRepository: ClickRepository

    @Inject
    lateinit var appRepository: AppRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            val packageName = event.packageName?.toString() ?: return
            if (packageName == "com.timetrace") return

            serviceScope.launch {
                try {
                    val timestamp = System.currentTimeMillis()
                    val date = dateFormat.format(Date(timestamp))

                    val appName = getAppName(packageName)
                    appRepository.getOrCreateApp(packageName, appName, timestamp)

                    val record = ClickRecordEntity(
                        packageName = packageName,
                        timestamp = timestamp,
                        date = date
                    )
                    clickRepository.insertRecord(record)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onInterrupt() {
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    companion object {
        var isRunning = false
            private set
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    init {
        isRunning = true
    }
}
