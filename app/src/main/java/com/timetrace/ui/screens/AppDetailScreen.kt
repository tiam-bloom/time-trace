package com.timetrace.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.timetrace.ui.components.AppIcon
import com.timetrace.ui.components.SimpleBarChart
import com.timetrace.ui.components.StatCard
import com.timetrace.ui.components.formatUsageTime
import com.timetrace.ui.viewmodel.AppDetailViewModel
import com.timetrace.ui.viewmodel.StatsPeriod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    onBack: () -> Unit,
    viewModel: AppDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.appName.ifEmpty { "应用详情" },
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(
                            packageName = uiState.packageName,
                            modifier = Modifier.size(48.dp)
                        )
                        Column {
                            Text(
                                text = uiState.appName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = periodLabel(uiState.period),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = periodTotalLabel(uiState.period),
                            value = formatUsageTime(uiState.totalUsageTime),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "日均时长",
                            value = formatUsageTime(uiState.averageDailyTime),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (uiState.chartData.isNotEmpty()) {
                        val barWidth = when (uiState.period) {
                            StatsPeriod.DAY -> 20.dp
                            StatsPeriod.WEEK -> 40.dp
                            StatsPeriod.MONTH -> 12.dp
                        }

                        SimpleBarChart(
                            data = uiState.chartData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            barWidth = barWidth
                        )

                        Text(
                            text = chartHint(uiState.period),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun periodLabel(period: StatsPeriod): String {
    return when (period) {
        StatsPeriod.DAY -> "日统计视图"
        StatsPeriod.WEEK -> "周统计视图"
        StatsPeriod.MONTH -> "月统计视图"
    }
}

private fun periodTotalLabel(period: StatsPeriod): String {
    return when (period) {
        StatsPeriod.DAY -> "今日时长"
        StatsPeriod.WEEK -> "本周时长"
        StatsPeriod.MONTH -> "本月时长"
    }
}

private fun chartHint(period: StatsPeriod): String {
    return when (period) {
        StatsPeriod.DAY -> "横轴：小时（0-23时），纵轴：使用时长"
        StatsPeriod.WEEK -> "横轴：日期（最近7天），纵轴：每日使用时长"
        StatsPeriod.MONTH -> "横轴：日期（最近30天），纵轴：每日使用时长"
    }
}
