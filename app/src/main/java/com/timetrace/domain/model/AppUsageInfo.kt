package com.timetrace.domain.model

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTime: Long,
    val clickCount: Int,
    val isUninstalled: Boolean = false
)
