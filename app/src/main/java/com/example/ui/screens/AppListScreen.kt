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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppCategory
import com.example.data.model.InstalledAppInfo
import com.example.ui.components.AppUsageCard

@Composable
fun AppListScreen(
    installedApps: List<InstalledAppInfo>,
    onSetLimitClick: (InstalledAppInfo) -> Unit,
    onToggleLimit: (InstalledAppInfo, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<AppCategory?>(null) }
    var showOnlyLimits by remember { mutableStateOf(false) }

    val filteredApps = remember(installedApps, searchQuery, selectedCategory, showOnlyLimits) {
        installedApps.filter { app ->
            val matchesSearch = searchQuery.isEmpty() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == null || app.category == selectedCategory
            val matchesLimitFilter = !showOnlyLimits || (app.limitMinutes > 0 && app.isEnabled)

            matchesSearch && matchesCategory && matchesLimitFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("app_list_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed applications...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_search_field")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null && !showOnlyLimits,
                    onClick = {
                        selectedCategory = null
                        showOnlyLimits = false
                    },
                    label = { Text("All Apps (${installedApps.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_all")
                )
            }

            item {
                FilterChip(
                    selected = showOnlyLimits,
                    onClick = {
                        showOnlyLimits = !showOnlyLimits
                        selectedCategory = null
                    },
                    label = { Text("With Limits (${installedApps.count { it.limitMinutes > 0 }})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.testTag("filter_limits_only")
                )
            }

            items(AppCategory.entries.toTypedArray()) { category ->
                val count = installedApps.count { it.category == category }
                FilterChip(
                    selected = selectedCategory == category && !showOnlyLimits,
                    onClick = {
                        selectedCategory = if (selectedCategory == category) null else category
                        showOnlyLimits = false
                    },
                    label = { Text("${category.displayName} ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                        selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "No apps found matching '$searchQuery'" else "No apps match the selected filter.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredApps, key = { it.packageName }) { appInfo ->
                    AppUsageCard(
                        appInfo = appInfo,
                        onSetLimitClick = { onSetLimitClick(appInfo) },
                        onToggleLimit = { isChecked -> onToggleLimit(appInfo, isChecked) }
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
