package com.timetrace.domain.model

data class TimeDistribution(
    val hour: Int,
    val usageTime: Long,
    val clickCount: Int
)
