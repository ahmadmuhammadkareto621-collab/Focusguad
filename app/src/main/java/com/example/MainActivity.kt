package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.db.AppLimitEntity
import com.example.data.model.InstalledAppInfo
import com.example.service.FocusAccessibilityService
import com.example.service.UsageMonitoringService
import com.example.ui.components.PinEntryDialog
import com.example.ui.components.SetLimitDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AppListScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start background usage monitoring service
        UsageMonitoringService.startService(this)

        setContent {
            val app = FocusGuardApplication.instance
            val isDarkTheme by app.userPreferencesRepository.isDarkThemeState.collectAsState()
            val isOnboarded by app.userPreferencesRepository.isOnboardedState.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                FocusGuardMainContainer(
                    isOnboarded = isOnboarded,
                    onSavePin = { pin -> app.userPreferencesRepository.savePin(pin) },
                    onCompleteOnboarding = { app.userPreferencesRepository.setOnboarded(true) },
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { enabled -> app.userPreferencesRepository.setDarkTheme(enabled) }
                )
            }
        }
    }
}

enum class NavigationDestination(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    APP_LIMITS("App Limits", Icons.Default.Apps, "nav_app_limits"),
    ANALYTICS("Analytics", Icons.Default.BarChart, "nav_analytics"),
    SETTINGS("Settings", Icons.Default.Settings, "nav_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusGuardMainContainer(
    isOnboarded: Boolean,
    onSavePin: (String) -> Unit,
    onCompleteOnboarding: () -> Unit,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val app = FocusGuardApplication.instance
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(NavigationDestination.DASHBOARD) }

    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var selectedAppForLimit by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var pendingLimitActionApp by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var showPinPromptForEdit by remember { mutableStateOf(false) }

    val dbLimits by app.appLimitRepository.allLimits.collectAsState(initial = emptyList())
    val usageLogs by app.usageStatsRepository.observeUsageLogsFromDate("2026-01-01").collectAsState(initial = emptyList())

    val hasUsagePermission = remember(context) { app.usageStatsRepository.hasUsageStatsPermission() }
    val hasAccessibilityPermission = remember(context) { FocusAccessibilityService.isAccessibilityServiceEnabled(context) }

    // Load installed apps and merge Room db limits
    LaunchedEffect(dbLimits, hasUsagePermission) {
        withContext(Dispatchers.IO) {
            val usageMap = app.usageStatsRepository.getTodayUsageStatsMap()
            val rawApps = app.usageStatsRepository.getInstalledApps(usageMap)
            val limitsMap = dbLimits.associateBy { it.packageName }

            val merged = rawApps.map { appInfo ->
                val limitEntity = limitsMap[appInfo.packageName]
                if (limitEntity != null) {
                    appInfo.copy(
                        limitMinutes = limitEntity.limitMinutes,
                        isEnabled = limitEntity.isEnabled,
                        tempUnlockUntilMs = limitEntity.tempUnlockUntilMs
                    )
                } else {
                    appInfo
                }
            }

            installedApps = merged.sortedByDescending { it.usageTimeMs }
            app.usageStatsRepository.syncTodayUsageToDatabase(merged)
        }
    }

    if (!isOnboarded) {
        OnboardingScreen(
            hasUsagePermission = hasUsagePermission,
            hasAccessibilityPermission = hasAccessibilityPermission,
            onSavePin = onSavePin,
            onCompleteOnboarding = onCompleteOnboarding
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = currentTab.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationDestination.entries.forEach { navItem ->
                        val isSelected = currentTab == navItem
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = navItem },
                            icon = { Icon(imageVector = navItem.icon, contentDescription = navItem.title) },
                            label = { Text(navItem.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag(navItem.tag)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (currentTab) {
                    NavigationDestination.DASHBOARD -> DashboardScreen(
                        installedApps = installedApps,
                        hasUsagePermission = hasUsagePermission,
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        onNavigateToAppList = { currentTab = NavigationDestination.APP_LIMITS },
                        onNavigateToAnalytics = { currentTab = NavigationDestination.ANALYTICS },
                        onNavigateToOnboarding = { onCompleteOnboarding() },
                        onSetLimitClick = { appInfo ->
                            if (app.userPreferencesRepository.hasPin()) {
                                pendingLimitActionApp = appInfo
                                showPinPromptForEdit = true
                            } else {
                                selectedAppForLimit = appInfo
                            }
                        },
                        onToggleLimit = { appInfo, enabled ->
                            scope.launch(Dispatchers.IO) {
                                val limitEntity = AppLimitEntity(
                                    packageName = appInfo.packageName,
                                    appName = appInfo.appName,
                                    limitMinutes = appInfo.limitMinutes,
                                    isEnabled = enabled,
                                    category = appInfo.category.displayName
                                )
                                app.appLimitRepository.toggleLimitEnabled(limitEntity, enabled)
                            }
                        }
                    )

                    NavigationDestination.APP_LIMITS -> AppListScreen(
                        installedApps = installedApps,
                        onSetLimitClick = { appInfo ->
                            if (app.userPreferencesRepository.hasPin()) {
                                pendingLimitActionApp = appInfo
                                showPinPromptForEdit = true
                            } else {
                                selectedAppForLimit = appInfo
                            }
                        },
                        onToggleLimit = { appInfo, enabled ->
                            scope.launch(Dispatchers.IO) {
                                val limitEntity = AppLimitEntity(
                                    packageName = appInfo.packageName,
                                    appName = appInfo.appName,
                                    limitMinutes = appInfo.limitMinutes,
                                    isEnabled = enabled,
                                    category = appInfo.category.displayName
                                )
                                app.appLimitRepository.toggleLimitEnabled(limitEntity, enabled)
                            }
                        }
                    )

                    NavigationDestination.ANALYTICS -> AnalyticsScreen(
                        installedApps = installedApps,
                        usageLogsHistory = usageLogs
                    )

                    NavigationDestination.SETTINGS -> SettingsScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme,
                        hasUsagePermission = hasUsagePermission,
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        onChangePinSuccess = { newPin -> app.userPreferencesRepository.savePin(newPin) },
                        onResetData = {
                            scope.launch(Dispatchers.IO) {
                                app.database.usageLogDao().clearAllLogs()
                            }
                        }
                    )
                }
            }
        }
    }

    // PIN Prompt for editing limit
    if (showPinPromptForEdit) {
        PinEntryDialog(
            title = "Enter Security PIN",
            subtitle = "Verify PIN to edit app limits",
            onVerifyPin = { pin -> app.userPreferencesRepository.verifyPin(pin) },
            onDismiss = {
                showPinPromptForEdit = false
                pendingLimitActionApp = null
            },
            onSuccess = {
                showPinPromptForEdit = false
                selectedAppForLimit = pendingLimitActionApp
                pendingLimitActionApp = null
            }
        )
    }

    // Set/Edit Limit Dialog
    if (selectedAppForLimit != null) {
        val appInfo = selectedAppForLimit!!
        SetLimitDialog(
            appInfo = appInfo,
            onDismiss = { selectedAppForLimit = null },
            onSaveLimit = { limitMins ->
                scope.launch(Dispatchers.IO) {
                    app.appLimitRepository.setAppLimit(
                        packageName = appInfo.packageName,
                        appName = appInfo.appName,
                        limitMinutes = limitMins,
                        category = appInfo.category.displayName
                    )
                }
                selectedAppForLimit = null
            },
            onRemoveLimit = {
                scope.launch(Dispatchers.IO) {
                    app.appLimitRepository.removeLimit(appInfo.packageName)
                }
                selectedAppForLimit = null
            }
        )
    }
}
