package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BarDataPoint(
    val label: String,      // e.g. "Mon", "Tue", "W1", "Jan"
    val valueMinutes: Long,
    val isHighlighted: Boolean = false
)

@Composable
fun BarChart(
    dataPoints: List<BarDataPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = MaterialTheme.colorScheme.tertiary
) {
    if (dataPoints.isEmpty()) return

    val maxVal = dataPoints.maxOfOrNull { it.valueMinutes }?.coerceAtLeast(60L) ?: 60L
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedIndex != null && selectedIndex!! in dataPoints.indices) {
            val point = dataPoints[selectedIndex!!]
            val hrs = point.valueMinutes / 60
            val mins = point.valueMinutes % 60
            Text(
                text = "${point.label}: ${if (hrs > 0) "${hrs}h " else ""}${mins}m",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dataPoints.forEachIndexed { index, point ->
                val ratio = (point.valueMinutes.toFloat() / maxVal.toFloat()).coerceIn(0.05f, 1f)
                val isSelected = selectedIndex == index
                val animatedRatio by animateFloatAsState(
                    targetValue = ratio,
                    animationSpec = tween(durationMillis = 600),
                    label = "BarHeightAnimation"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { selectedIndex = index },
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(if (dataPoints.size > 12) 12.dp else 22.dp)
                            .fillMaxHeight(animatedRatio)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.secondary
                                    point.isHighlighted -> highlightColor
                                    else -> barColor
                                }
                            )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = if (dataPoints.size > 12) 9.sp else 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
