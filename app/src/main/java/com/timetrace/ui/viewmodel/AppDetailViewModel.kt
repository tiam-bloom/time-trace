package com.timetrace.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrace.data.local.dao.DailyTotal
import com.timetrace.data.repository.AppRepository
import com.timetrace.data.repository.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AppDetailUiState(
    val packageName: String = "",
    val appName: String = "",
    val period: StatsPeriod = StatsPeriod.WEEK,
    val chartData: List<Pair<String, Long>> = emptyList(),
    val totalUsageTime: Long = 0,
    val averageDailyTime: Long = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usageRepository: UsageRepository,
    private val appRepository: AppRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val packageName: String = savedStateHandle["packageName"] ?: ""
    private val periodName: String = savedStateHandle["period"] ?: StatsPeriod.WEEK.name
    private val period = StatsPeriod.valueOf(periodName)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val _uiState = MutableStateFlow(AppDetailUiState())
    val uiState: StateFlow<AppDetailUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val app = appRepository.getAppByPackage(packageName)
            val appName = if (app != null && app.appName != app.packageName) {
                app.appName
            } else {
                getAppNameFromPackageManager(packageName)
            }

            val today = dateFormat.format(Date())
            when (period) {
                StatsPeriod.DAY -> loadDayData(today, appName)
                StatsPeriod.WEEK -> loadRangeData(getPastDaysRange(6), appName)
                StatsPeriod.MONTH -> loadRangeData(getPastDaysRange(29), appName)
            }
        }
    }

    private suspend fun loadDayData(today: String, appName: String) {
        usageRepository.getUsageRecordsByDate(today).collect { records ->
            val filtered = records.filter { it.packageName == packageName }
            val hourlyMap = mutableMapOf<Int, Long>()
            val cal = Calendar.getInstance()
            filtered.forEach { record ->
                cal.timeInMillis = record.startTime
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                hourlyMap[hour] = (hourlyMap[hour] ?: 0) + record.duration
            }
            val chartData = (0..23).map { hour ->
                "$hour" to (hourlyMap[hour] ?: 0)
            }
            val total = chartData.sumOf { it.second }
            _uiState.value = AppDetailUiState(
                packageName = packageName,
                appName = appName,
                period = period,
                chartData = chartData,
                totalUsageTime = total,
                averageDailyTime = total,
                isLoading = false
            )
        }
    }

    private suspend fun loadRangeData(range: Pair<String, String>, appName: String) {
        val (startDate, endDate) = range
        val isMonth = period == StatsPeriod.MONTH
        val labelInterval = if (isMonth) 5 else 1
        usageRepository.getPackageDailyTotals(packageName, startDate, endDate).collect { dailyTotals ->
            val chartData = fillMissingDates(dailyTotals, startDate, endDate, labelInterval)
            val total = chartData.sumOf { it.second }
            val days = chartData.size
            _uiState.value = AppDetailUiState(
                packageName = packageName,
                appName = appName,
                period = period,
                chartData = chartData,
                totalUsageTime = total,
                averageDailyTime = if (days > 0) total / days else 0,
                isLoading = false
            )
        }
    }

    private fun fillMissingDates(
        dailyTotals: List<DailyTotal>,
        startDate: String,
        endDate: String,
        labelInterval: Int = 1
    ): List<Pair<String, Long>> {
        val dataMap = dailyTotals.associate { it.date to it.totalDuration }
        val cal = Calendar.getInstance()
        cal.time = dateFormat.parse(startDate)!!
        val endCal = Calendar.getInstance()
        endCal.time = dateFormat.parse(endDate)!!

        val dateLabelFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        val dayOnlyFormat = SimpleDateFormat("d", Locale.getDefault())
        var dayIndex = 0
        val result = mutableListOf<Pair<String, Long>>()
        while (!cal.after(endCal)) {
            val dateStr = dateFormat.format(cal.time)
            val label = if (labelInterval > 1) {
                if (dayIndex % labelInterval == 0) dayOnlyFormat.format(cal.time) else ""
            } else {
                dateLabelFormat.format(cal.time)
            }
            result.add(label to (dataMap[dateStr] ?: 0))
            cal.add(Calendar.DAY_OF_MONTH, 1)
            dayIndex++
        }
        return result
    }

    private fun getPastDaysRange(daysBack: Int): Pair<String, String> {
        val cal = Calendar.getInstance()
        val endDate = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, -daysBack)
        val startDate = dateFormat.format(cal.time)
        return startDate to endDate
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
}
