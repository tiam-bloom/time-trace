package com.timetrace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 8.dp)
                .let { if (barWidth != null) it.horizontalScroll(scrollState) else it },
            horizontalArrangement = if (barWidth != null) Arrangement.spacedBy(2.dp) else Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            data.forEachIndexed { index, (_, value) ->
                val heightFraction = if (maxValue > 0) value.toFloat() / maxValue else 0f
                Box(
                    modifier = Modifier
                        .let { if (barWidth != null) it.width(barWidth) else it.weight(1f) }
                        .fillMaxHeight(heightFraction.coerceIn(0.03f, 1f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(ChartColors[index % ChartColors.size])
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .let { if (barWidth != null) it.horizontalScroll(scrollState) else it },
            horizontalArrangement = if (barWidth != null) Arrangement.spacedBy(2.dp) else Arrangement.SpaceEvenly
        ) {
            data.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.let { if (barWidth != null) it.width(barWidth) else it.weight(1f) },
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
