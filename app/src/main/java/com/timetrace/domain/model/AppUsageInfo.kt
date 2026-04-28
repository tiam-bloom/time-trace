package com.timetrace.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTime: Long,
    val clickCount: Int,
    val launchCount: Int = 0,
    val isUninstalled: Boolean = false
)
