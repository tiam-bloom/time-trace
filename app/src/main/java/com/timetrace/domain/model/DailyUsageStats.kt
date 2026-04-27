package com.timetrace.domain.model

data class DailyUsageStats(
    val date: String,
    val totalUsageTime: Long,
    val totalClicks: Int,
    val unlockCount: Int,
    val topApps: List<AppUsageInfo>
)
