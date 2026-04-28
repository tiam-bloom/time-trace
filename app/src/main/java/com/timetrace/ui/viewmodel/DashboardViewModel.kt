package com.timetrace.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrace.data.local.dao.PackageDuration
import com.timetrace.data.repository.AppRepository
import com.timetrace.data.repository.ClickRepository
import com.timetrace.data.repository.UnlockRepository
import com.timetrace.data.repository.UsageRepository
import com.timetrace.domain.model.AppUsageInfo
import com.timetrace.service.UsageStatsService
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
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Immutable
data class DashboardUiState(
    val todayUsageTime: Long = 0,
    val todayClicks: Int = 0,
    val todayUnlocks: Int = 0,
    val todayLaunches: Int = 0,
    val topApps: List<AppUsageInfo> = emptyList(),
    val isLoading: Boolean = true,
    val hasUsagePermission: Boolean = false,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val clickRepository: ClickRepository,
    private val unlockRepository: UnlockRepository,
    private val appRepository: AppRepository,
    private val usageStatsService: UsageStatsService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today = dateFormat.format(Date())
    private val appNameCache = mutableMapOf<String, String>()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            appRepository.refreshAllAppNames()
            appNameCache.clear()
            loadDashboardData()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            val hasPermission = usageStatsService.hasUsageStatsPermission()

            combine(
                usageRepository.getTotalUsageTimeByDate(today),
                clickRepository.getTotalClicksByDate(today),
                unlockRepository.getUnlockCountByDate(today),
                combine(
                    usageRepository.getTopApps(today, 5),
                    usageRepository.getDailyLaunchSummary(today)
                ) { topApps, launchSummary -> topApps to launchSummary },
                usageRepository.getTotalLaunchesByDate(today)
            ) { usageTime, clicks, unlocks, topAppsPair, launches ->
                withContext(Dispatchers.Default) {
                    val (topApps, launchSummary) = topAppsPair
                    val launchMap = launchSummary.associate { it.packageName to it.launchCount }
                    DashboardUiState(
                        todayUsageTime = usageTime ?: 0,
                        todayClicks = clicks,
                        todayUnlocks = unlocks,
                        todayLaunches = launches,
                        topApps = topApps.map { it.toAppUsageInfo(launchMap) },
                        isLoading = false,
                        hasUsagePermission = hasPermission
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    private suspend fun PackageDuration.toAppUsageInfo(launchMap: Map<String, Int>): AppUsageInfo {
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
            launchCount = launchMap[packageName] ?: 0,
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
