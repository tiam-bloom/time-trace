package com.timetrace.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrace.data.repository.AppRepository
import com.timetrace.data.repository.ClickRepository
import com.timetrace.data.repository.UsageRepository
import com.timetrace.domain.model.AppUsageInfo
import com.timetrace.service.UsageStatsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AppListUiState(
    val apps: List<AppUsageInfo> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val clickRepository: ClickRepository,
    private val appRepository: AppRepository,
    private val usageStatsService: UsageStatsService
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today = dateFormat.format(Date())

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    init {
        loadAppList()
    }

    fun refreshAppList() {
        viewModelScope.launch {
            if (usageStatsService.hasUsageStatsPermission()) {
                try {
                    usageStatsService.collectUsageStats()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            loadAppList()
        }
    }

    private fun loadAppList() {
        viewModelScope.launch {
            combine(
                usageRepository.getDailyUsageSummary(today),
                clickRepository.getDailyClickSummary(today),
                appRepository.getAllApps()
            ) { usageSummary, clickSummary, apps ->
                val appUsageMap = usageSummary.associate { it.packageName to it.totalDuration }
                val clickCountMap = clickSummary.associate { it.packageName to it.clickCount }

                apps.map { appEntity ->
                    AppUsageInfo(
                        packageName = appEntity.packageName,
                        appName = appEntity.appName,
                        usageTime = appUsageMap[appEntity.packageName] ?: 0,
                        clickCount = clickCountMap[appEntity.packageName] ?: 0,
                        isUninstalled = appEntity.isUninstalled
                    )
                }.sortedByDescending { it.usageTime }
            }.collect { appList ->
                _uiState.value = AppListUiState(
                    apps = appList,
                    isLoading = false
                )
            }
        }
    }
}
