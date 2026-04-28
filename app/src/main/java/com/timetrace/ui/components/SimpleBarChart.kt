package com.timetrace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.timetrace.ui.theme.ChartColors

@Composable
fun SimpleBarChart(
    data: List<Pair<String, Long>>,
    modifier: Modifier = Modifier,
    maxValue: Long = data.maxOfOrNull { it.second } ?: 1L,
    barWidth: Dp? = null
) {
    val scrollState = rememberScrollState()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val yAxisWidth = 48.dp

    val yAxisLabels = remember(maxValue) { calculateYAxisTicks(maxValue) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedIndex != null) {
            val (label, value) = data[selectedIndex!!]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$label  ${formatUsageTime(value)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .width(yAxisWidth)
                    .fillMaxHeight()
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                yAxisLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .let { if (barWidth != null) it.horizontalScroll(scrollState) else it },
                    horizontalArrangement = if (barWidth != null) Arrangement.spacedBy(2.dp) else Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    data.forEachIndexed { index, (_, value) ->
                        val heightFraction = if (maxValue > 0) value.toFloat() / maxValue else 0f
                        val isSelected = selectedIndex == index

                        Box(
                            modifier = Modifier
                                .let {
                                    if (barWidth != null) it.width(barWidth)
                                    else it.weight(1f).padding(horizontal = 2.dp)
                                }
                                .fillMaxHeight(heightFraction.coerceIn(0.03f, 1f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(ChartColors[index % ChartColors.size])
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    ) else Modifier
                                )
                                .clickable {
                                    selectedIndex = if (isSelected) null else index
                                }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .let { if (barWidth != null) it.horizontalScroll(scrollState) else it },
                    horizontalArrangement = if (barWidth != null) Arrangement.spacedBy(2.dp) else Arrangement.SpaceEvenly
                ) {
                    data.forEach { (label, _) ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.let {
                                if (barWidth != null) it.width(barWidth) else it.weight(1f)
                            },
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun calculateYAxisTicks(maxValue: Long): List<String> {
    return (0..4).map { i ->
        val value = maxValue * (4 - i) / 4
        formatShortDuration(value)
    }
}

private fun formatShortDuration(millis: Long): String {
    val totalMinutes = millis / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m"
        else -> "${millis / 1000}s"
    }
}
