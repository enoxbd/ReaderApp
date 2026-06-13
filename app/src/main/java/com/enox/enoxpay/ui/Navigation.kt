package com.enox.enoxpay.ui

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.enox.enoxpay.service.ApiWorker
import com.enox.enoxpay.ui.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.enox.enoxpay.ui.components.EnoxToast

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EnoxApp() {
    val navController = rememberNavController()
    val isGatewayEnabled by com.enox.enoxpay.di.Graph.appPreferencesManager.isGatewayEnabled.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("splash") { SplashScreen(navController = navController) }
            composable("dashboard") { DashboardScreen(navController = navController) }
            composable("api_config") { ApiConfigScreen(navController) }
            composable("regex_config") { RegexManagerScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
            composable("diagnostics") { DiagnosticsScreen(navController) }
            composable("developer_docs") { DeveloperDocsScreen(navController) }
            composable("permissions") { PermissionCenterScreen() }
            composable("inbox") { InboxExplorerScreen() }
            composable("manual_transaction") { ManualTransactionScreen() }
            composable("trash") { TrashScreen(navController) }
            composable("transactions") { TransactionsScreen(navController) }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        if (currentRoute in listOf("dashboard", "inbox", "regex_config", "settings")) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                BottomNavigation(navController = navController)
            }
        }
        
        EnoxToast()
    }
}

@Composable
fun BottomNavigation(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        NavigationBar(
            tonalElevation = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    RoundedCornerShape(24.dp)
                )
                .background(Color.Transparent, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp)),
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Dashboard") },
                label = { Text("DASH", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) },
                selected = currentRoute == "dashboard",
                onClick = { navController.navigate("dashboard") { launchSingleTop = true; restoreState = true } },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Email, contentDescription = "Inbox") },
                label = { Text("INBOX", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) },
                selected = currentRoute == "inbox",
                onClick = { navController.navigate("inbox") { launchSingleTop = true; restoreState = true } },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Build, contentDescription = "Platform") },
                label = { Text("PLATFORM", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) },
                selected = currentRoute == "regex_config",
                onClick = { navController.navigate("regex_config") { launchSingleTop = true; restoreState = true } },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("SETTINGS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) },
                selected = currentRoute == "settings" || currentRoute == "api_config" || currentRoute == "permissions" || currentRoute == "manual_transaction",
                onClick = { navController.navigate("settings") { launchSingleTop = true; restoreState = true } },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    val allSms by viewModel.allSms.collectAsState()
    val context = LocalContext.current
    val isGatewayEnabled by com.enox.enoxpay.di.Graph.appPreferencesManager.isGatewayEnabled.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL TIME") }
    val timeFilters = listOf("TODAY", "WEEK", "MONTH", "ALL TIME")

    val currentTime = System.currentTimeMillis()
    val filterThreshold = when (selectedFilter) {
        "TODAY" -> currentTime - (24 * 60 * 60 * 1000L)
        "WEEK" -> currentTime - (7 * 24 * 60 * 60 * 1000L)
        "MONTH" -> currentTime - (30L * 24 * 60 * 60 * 1000L)
        else -> 0L
    }

    val filteredSmsForStats = allSms.filter { it.timestamp >= filterThreshold }

    val successCount = filteredSmsForStats.count { it.status == "SUCCESS" }
    val failedCount = filteredSmsForStats.count { it.status == "FAILED" }
    val pendingCount = filteredSmsForStats.count { it.status == "PENDING" }
    val totalVolume = filteredSmsForStats.filter { it.status == "SUCCESS" }.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.enox.enoxpay.R.drawable.enoxpay_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Text(
                        text = "Enox Pay",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                val statusColor = if (isGatewayEnabled) com.enox.enoxpay.ui.theme.SuccessGreen else MaterialTheme.colorScheme.error
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(color = statusColor)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isGatewayEnabled) "SERVICE ACTIVE" else "SERVICE INACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }
            IconButton(
                onClick = { ApiWorker.enqueue(context) },
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape = CircleShape)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (!isGatewayEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gateway is Inactive",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enable it in settings to process incoming SMS.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = { navController.navigate("settings") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Enable", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Time Filters
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(timeFilters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Hero Analytics Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Column {
                    Text(
                        text = "Processing Volume ($selectedFilter)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "৳ %,.2f".format(totalVolume),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text(
                                text = "SUCCESSFUL TXNS",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$successCount",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "FAILED",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$failedCount",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.errorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Stats Grid
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Success Stat
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(40.dp).background(com.enox.enoxpay.ui.theme.SuccessBg, shape = RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(text = "SUCCESS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(text = "$successCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            // Queue Stat
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(40.dp).background(com.enox.enoxpay.ui.theme.QueueBg, shape = RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = com.enox.enoxpay.ui.theme.QueueOrange, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(text = "QUEUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(text = "$pendingCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Logs Section
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("RECENT DETECTION", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            TextButton(onClick = { navController.navigate("transactions") }) {
                Text("VIEW ALL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        if (allSms.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No recent transaction found", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(allSms) { sms ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        val platformInitial = sms.platformName?.firstOrNull()?.toString()?.uppercase() ?: "?"
                        val platformColor = when(sms.platformName?.lowercase()) {
                            "bkash" -> com.enox.enoxpay.ui.theme.BkashColor
                            "nagad" -> com.enox.enoxpay.ui.theme.NagadColor
                            "rocket" -> com.enox.enoxpay.ui.theme.RocketColor
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.size(52.dp).background(platformColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                    Text(platformInitial, color = platformColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                                }
                                Column {
                                    Text(sms.platformName ?: "Unknown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    val date = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(sms.timestamp))
                                    Text("TXN: ${sms.transactionId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 120.dp)) {
                                Text(
                                    text = "+৳ ${sms.amount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    color = if (sms.status == "SUCCESS") com.enox.enoxpay.ui.theme.SuccessGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val statusBgColor = if (sms.status == "SUCCESS") com.enox.enoxpay.ui.theme.SuccessBg else if (sms.status == "FAILED") MaterialTheme.colorScheme.errorContainer else com.enox.enoxpay.ui.theme.QueueBg
                                val statusTextColor = if (sms.status == "SUCCESS") com.enox.enoxpay.ui.theme.SuccessGreen else if (sms.status == "FAILED") MaterialTheme.colorScheme.error else com.enox.enoxpay.ui.theme.QueueOrange
                                Box(modifier = Modifier.background(statusBgColor, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                    Text(sms.status, style = MaterialTheme.typography.labelSmall, color = statusTextColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

// ApiConfigScreen is now in its own file
