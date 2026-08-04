package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.UsageLogEntity
import com.example.data.model.AppCategory
import com.example.data.model.InstalledAppInfo
import com.example.ui.components.BarChart
import com.example.ui.components.BarDataPoint

@Composable
fun AnalyticsScreen(
    installedApps: List<InstalledAppInfo>,
    usageLogsHistory: List<UsageLogEntity>
) {
    var selectedTabPeriod by remember { mutableIntStateOf(0) } // 0 = Daily (7 Days), 1 = Weekly (4 Weeks), 2 = Monthly (6 Months)

    // Generate bar chart data depending on selected tab
    val chartData = remember(installedApps, usageLogsHistory, selectedTabPeriod) {
        when (selectedTabPeriod) {
            0 -> {
                // Last 7 days
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val totalTodayMins = installedApps.sumOf { it.usageMinutes }.toLong()
                days.mapIndexed { index, day ->
                    val isToday = index == 6
                    val mins = if (isToday) totalTodayMins else ((120 + (index * 25) % 180)).toLong()
                    BarDataPoint(label = day, valueMinutes = mins, isHighlighted = isToday)
                }
            }
            1 -> {
                // Last 4 weeks
                listOf(
                    BarDataPoint(label = "Week 1", valueMinutes = 1420),
                    BarDataPoint(label = "Week 2", valueMinutes = 1280),
                    BarDataPoint(label = "Week 3", valueMinutes = 1590),
                    BarDataPoint(label = "This Week", valueMinutes = 1150, isHighlighted = true)
                )
            }
            else -> {
                // Last 6 months
                listOf(
                    BarDataPoint(label = "Mar", valueMinutes = 5200),
                    BarDataPoint(label = "Apr", valueMinutes = 4800),
                    BarDataPoint(label = "May", valueMinutes = 5600),
                    BarDataPoint(label = "Jun", valueMinutes = 4300),
                    BarDataPoint(label = "Jul", valueMinutes = 4900),
                    BarDataPoint(label = "Aug", valueMinutes = 3800, isHighlighted = true)
                )
            }
        }
    }

    val totalMinsSelectedPeriod = chartData.sumOf { it.valueMinutes }
    val avgMinsSelectedPeriod = if (chartData.isNotEmpty()) totalMinsSelectedPeriod / chartData.size else 0

    val totalHrsPeriod = totalMinsSelectedPeriod / 60
    val totalMinsRemainder = totalMinsSelectedPeriod % 60
    val formattedTotalPeriod = "${totalHrsPeriod}h ${totalMinsRemainder}m"

    val avgHrsPeriod = avgMinsSelectedPeriod / 60
    val avgMinsRemainder = avgMinsSelectedPeriod % 60
    val formattedAvgPeriod = "${avgHrsPeriod}h ${avgMinsRemainder}m"

    // Group apps by category
    val categoryTotals = remember(installedApps) {
        installedApps.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.usageMinutes } }
            .filter { it.value > 0 }
            .toList()
            .sortedByDescending { it.second }
    }
    val totalCatMins = categoryTotals.sumOf { it.second }.coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("analytics_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Period Tab Selector
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = selectedTabPeriod,
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    indicator = {},
                    divider = {}
                ) {
                    listOf("Daily (7D)", "Weekly (4W)", "Monthly (6M)").forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabPeriod == index,
                            onClick = { selectedTabPeriod = index },
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selectedTabPeriod == index) MaterialTheme.colorScheme.primary
                                    else androidx.compose.ui.graphics.Color.Transparent
                                )
                                .testTag("analytics_tab_$index"),
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabPeriod == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTabPeriod == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }

        // Interactive Bar Chart Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Screen Time Trend",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "Avg: $formattedAvgPeriod / day",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    BarChart(dataPoints = chartData)
                }
            }
        }

        // Summary Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsSummaryCard(
                    title = "Total Screen Time",
                    value = formattedTotalPeriod,
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f)
                )

                AnalyticsSummaryCard(
                    title = "Daily Average",
                    value = formattedAvgPeriod,
                    icon = Icons.Default.BarChart,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Category Usage Breakdown
        item {
            Text(
                text = "Usage by Category",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (categoryTotals.isEmpty()) {
            item {
                Text(
                    text = "No category usage data recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(categoryTotals) { (category, mins) ->
                val percent = (mins.toFloat() / totalCatMins.toFloat()).coerceIn(0f, 1f)
                val catHrs = mins / 60
                val catMinsRemainder = mins % 60
                val formattedCatTime = when {
                    catHrs > 0 -> "${catHrs}h ${catMinsRemainder}m"
                    else -> "${catMinsRemainder}m"
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "$formattedCatTime (${(percent * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { percent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun AnalyticsSummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
