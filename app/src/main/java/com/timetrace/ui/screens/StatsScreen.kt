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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.timetrace.ui.components.AppListItem
import com.timetrace.ui.components.SimpleBarChart
import com.timetrace.ui.components.StatCard
import com.timetrace.ui.components.formatUsageTime
import com.timetrace.ui.viewmodel.StatsPeriod
import com.timetrace.ui.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "使用统计",
                        fontWeight = FontWeight.Bold
                    )
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
                    PeriodSelector(
                        selectedPeriod = uiState.selectedPeriod,
                        onPeriodSelected = { viewModel.selectPeriod(it) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = periodTotalLabel(uiState.selectedPeriod),
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
                        val barWidth = when (uiState.selectedPeriod) {
                            StatsPeriod.DAY -> 20.dp
                            StatsPeriod.WEEK -> 40.dp
                            StatsPeriod.MONTH -> 10.dp
                        }

                        SimpleBarChart(
                            data = uiState.chartData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            barWidth = barWidth
                        )

                        Text(
                            text = chartHint(uiState.selectedPeriod),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    if (uiState.appUsageList.isNotEmpty()) {
                        Text(
                            text = "应用排行",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        uiState.appUsageList.forEachIndexed { index, app ->
                            AppListItem(
                                app = app,
                                rank = index + 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: StatsPeriod,
    onPeriodSelected: (StatsPeriod) -> Unit
) {
    val periods = listOf(
        StatsPeriod.DAY to "日",
        StatsPeriod.WEEK to "周",
        StatsPeriod.MONTH to "月"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        periods.forEach { (period, label) ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
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
