package com.timetrace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info")
data class AppInfoEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val firstSeenTime: Long,
    val lastSeenTime: Long,
    val isUninstalled: Boolean = false
)
