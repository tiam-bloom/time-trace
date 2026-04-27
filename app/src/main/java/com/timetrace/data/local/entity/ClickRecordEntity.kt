package com.timetrace.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "click_record",
    indices = [Index(value = ["packageName", "timestamp"])]
)
data class ClickRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val timestamp: Long,
    val date: String
)
