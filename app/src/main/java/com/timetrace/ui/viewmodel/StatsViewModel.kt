package com.timetrace.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrace.data.local.dao.DailyTotal
import com.timetrace.data.local.dao.PackageDuration
import com.timetrace.data.repository.AppRepository
import com.timetrace.data.repository.UsageRepository
import com.timetrace.domain.model.AppUsageInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class StatsPeriod { DAY, WEEK, MONTH }

@Immutable
data class StatsUiState(
    val selectedPeriod: StatsPeriod = StatsPeriod.WEEK,
    val chartData: List<Pair<String, Long>> = emptyList(),
    val totalUsageTime: Long = 0,
    val averageDailyTime: Long = 0,
    val appUsageList: List<AppUsageInfo> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val appRepository: AppRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateLabelFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    private val dayOnlyFormat = SimpleDateFormat("d", Locale.getDefault())
    private val calendar = Calendar.getInstance()
    private val appNameCache = mutableMapOf<String, String>()

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun selectPeriod(period: StatsPeriod) {
        if (_uiState.value.selectedPeriod != period) {
            _uiState.value = _uiState.value.copy(selectedPeriod = period, isLoading = true)
            loadData()
        }
    }

    private fun loadData() {
        val today = dateFormat.format(Date())
        val period = _uiState.value.selectedPeriod

        when (period) {
            StatsPeriod.DAY -> loadDayData(today)
            StatsPeriod.WEEK -> loadRangeData(getPastDaysRange(6))
            StatsPeriod.MONTH -> loadRangeData(getPastDaysRange(29))
        }
    }

    private fun loadDayData(today: String) {
        viewModelScope.launch {
            combine(
                usageRepository.getUsageRecordsByDate(today),
                usageRepository.getDailyUsageSummary(today)
            ) { records, packageDurations ->
                withContext(Dispatchers.Default) {
                    val hourlyMap = mutableMapOf<Int, Long>()
                    val cal = Calendar.getInstance()
                    records.forEach { record ->
                        cal.timeInMillis = record.startTime
                        val hour = cal.get(Calendar.HOUR_OF_DAY)
                        hourlyMap[hour] = (hourlyMap[hour] ?: 0) + record.duration
                    }
                    val chartData = (0..23).map { hour ->
                        "$hour" to (hourlyMap[hour] ?: 0)
                    }
                    val total = chartData.sumOf { it.second }
                    val appList = packageDurations.map { it.toAppUsageInfo() }
                    StatsUiState(
                        selectedPeriod = StatsPeriod.DAY,
                        chartData = chartData,
                        totalUsageTime = total,
                        averageDailyTime = total,
                        appUsageList = appList,
                        isLoading = false
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun loadRangeData(range: Pair<String, String>) {
        val (startDate, endDate) = range
        val isMonth = _uiState.value.selectedPeriod == StatsPeriod.MONTH
        val labelInterval = if (isMonth) 5 else 1
        viewModelScope.launch {
            combine(
                usageRepository.getDailyTotals(startDate, endDate),
                usageRepository.getWeeklyUsageSummary(startDate, endDate)
            ) { dailyTotals, packageDurations ->
                withContext(Dispatchers.Default) {
                    val chartData = fillMissingDates(dailyTotals, startDate, endDate, labelInterval)
                    val total = chartData.sumOf { it.second }
                    val days = chartData.size
                    val appList = packageDurations.map { it.toAppUsageInfo() }
                    StatsUiState(
                        selectedPeriod = _uiState.value.selectedPeriod,
                        chartData = chartData,
                        totalUsageTime = total,
                        averageDailyTime = if (days > 0) total / days else 0,
                        appUsageList = appList,
                        isLoading = false
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private fun fillMissingDates(
        dailyTotals: List<DailyTotal>,
        startDate: String,
        endDate: String,
        labelInterval: Int = 1
    ): List<Pair<String, Long>> {
        val dataMap = dailyTotals.associate { it.date to it.totalDuration }
        val cal = calendar.clone() as Calendar
        cal.time = dateFormat.parse(startDate)!!
        val endCal = calendar.clone() as Calendar
        endCal.time = dateFormat.parse(endDate)!!

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
        val cal = calendar.clone() as Calendar
        val endDate = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, -daysBack)
        val startDate = dateFormat.format(cal.time)
        return startDate to endDate
    }

    private suspend fun PackageDuration.toAppUsageInfo(): AppUsageInfo {
        val app = appRepository.getAppByPackage(packageName)
        val appName = if (app != null && app.appName != app.packageName) {
            app.appName
        } else {
            appNameCache.getOrPut(packageName) {
                withContext(Dispatchers.IO) { getAppNameFromPackageManager(packageName) }
            }
        }
        return AppUsageInfo(
            packageName = packageName,
            appName = appName,
            usageTime = totalDuration,
            clickCount = 0,
            isUninstalled = app?.isUninstalled ?: false
        )
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
