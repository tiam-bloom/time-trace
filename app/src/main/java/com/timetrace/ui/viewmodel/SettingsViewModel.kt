package com.timetrace.ui.viewmodel

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrace.data.local.dao.AppInfoDao
import com.timetrace.data.local.dao.ClickRecordDao
import com.timetrace.data.local.dao.UnlockRecordDao
import com.timetrace.data.local.dao.UsageRecordDao
import com.timetrace.domain.usecase.ExportDataUseCase
import com.timetrace.service.ClickAccessibilityService
import com.timetrace.service.UsageStatsService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val isClickTrackingEnabled: Boolean = false,
    val hasUsageStatsPermission: Boolean = false,
    val isExporting: Boolean = false,
    val isClearing: Boolean = false,
    val snackbarMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsService: UsageStatsService,
    private val exportDataUseCase: ExportDataUseCase,
    private val usageRecordDao: UsageRecordDao,
    private val clickRecordDao: ClickRecordDao,
    private val unlockRecordDao: UnlockRecordDao,
    private val appInfoDao: AppInfoDao
) : ViewModel() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        checkAccessibilityService()
        checkUsageStatsPermission()
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun exportData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val today = dateFormat.format(Date())
            // Export last 30 days
            val endDate = today
            val startDate = dateFormat.format(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))

            val file = exportDataUseCase.exportToCsv(startDate, endDate)
            if (file != null) {
                exportDataUseCase.shareFile(file)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    snackbarMessage = "数据已导出"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    snackbarMessage = "导出失败"
                )
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClearing = true)
            try {
                usageRecordDao.deleteAll()
                clickRecordDao.deleteAll()
                unlockRecordDao.deleteAll()
                appInfoDao.deleteAll()
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    snackbarMessage = "数据已清除"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isClearing = false,
                    snackbarMessage = "清除失败"
                )
            }
        }
    }

    private fun checkAccessibilityService() {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val isEnabled = enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName &&
            it.resolveInfo.serviceInfo.name == ClickAccessibilityService::class.java.name
        }
        _uiState.value = _uiState.value.copy(isClickTrackingEnabled = isEnabled)
    }

    private fun checkUsageStatsPermission() {
        val hasPermission = usageStatsService.hasUsageStatsPermission()
        _uiState.value = _uiState.value.copy(hasUsageStatsPermission = hasPermission)
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun openUsageStatsSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
