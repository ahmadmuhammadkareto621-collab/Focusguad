package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.model.AppCategory
import com.example.data.model.InstalledAppInfo

@Composable
fun AppUsageCard(
    appInfo: InstalledAppInfo,
    onSetLimitClick: () -> Unit,
    onToggleLimit: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val hrs = appInfo.usageMinutes / 60
    val mins = appInfo.usageMinutes % 60
    val formattedUsage = when {
        hrs > 0 -> "${hrs}h ${mins}m"
        mins > 0 -> "${mins}m"
        else -> "< 1m"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("app_card_${appInfo.packageName}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon or Category Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (appInfo.icon != null) {
                        Image(
                            bitmap = appInfo.icon.toBitmap().asImageBitmap(),
                            contentDescription = appInfo.appName,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        val categoryIcon = when (appInfo.category) {
                            AppCategory.SOCIAL -> Icons.Default.Share
                            AppCategory.ENTERTAINMENT -> Icons.Default.PlayArrow
                            AppCategory.GAMES -> Icons.Default.Games
                            AppCategory.COMMUNICATION -> Icons.Default.Chat
                            AppCategory.PRODUCTIVITY -> Icons.Default.Work
                            AppCategory.GENERAL -> Icons.Default.Category
                        }
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = appInfo.appName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = appInfo.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedUsage,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Limit Badge or Setup Button
                if (appInfo.limitMinutes > 0) {
                    Switch(
                        checked = appInfo.isEnabled,
                        onCheckedChange = onToggleLimit,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("limit_switch_${appInfo.packageName}")
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(onClick = onSetLimitClick)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("set_limit_btn_${appInfo.packageName}")
                    ) {
                        Text(
                            text = "+ Limit",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Progress Bar if limit set
            if (appInfo.limitMinutes > 0) {
                Spacer(modifier = Modifier.height(12.dp))

                val limitHrs = appInfo.limitMinutes / 60
                val limitMins = appInfo.limitMinutes % 60
                val formattedLimit = when {
                    limitHrs > 0 -> "${limitHrs}h ${limitMins}m"
                    else -> "${limitMins}m"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Limit: $formattedLimit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val statusText = when {
                        appInfo.isLimitExceeded -> "Blocked"
                        appInfo.isWarningThresholdReached -> "Warning (< 5m left)"
                        else -> "${((1f - appInfo.progressPercent) * appInfo.limitMinutes).toInt()}m left"
                    }

                    val statusColor = when {
                        appInfo.isLimitExceeded -> MaterialTheme.colorScheme.error
                        appInfo.isWarningThresholdReached -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { appInfo.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        appInfo.isLimitExceeded -> MaterialTheme.colorScheme.error
                        appInfo.isWarningThresholdReached -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
