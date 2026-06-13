package com.enox.enoxpay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

import androidx.compose.runtime.*
import com.enox.enoxpay.di.Graph
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.core.content.ContextCompat
import android.provider.Settings

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val themeMode by Graph.themeManager.themeMode.collectAsState()
    val isGatewayEnabled by Graph.appPreferencesManager.isGatewayEnabled.collectAsState()
    val context = LocalContext.current
    
    val permissionsState = rememberMultiplePermissionsState(
        permissions = buildList {
            add(android.Manifest.permission.RECEIVE_SMS)
            add(android.Manifest.permission.READ_SMS)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsGroup(title = "Display") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DarkMode, contentDescription = "Dark Mode", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Dark Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(when(themeMode) { 1 -> "On"; else -> "Off" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = themeMode == 1,
                        onCheckedChange = { isChecked ->
                            Graph.themeManager.setThemeMode(if (isChecked) 1 else 0)
                        }
                    )
                }
            }
            
            SettingsGroup(title = "General") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Gateway", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Gateway Service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(if (isGatewayEnabled) "Running" else "Stopped", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isGatewayEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
                                val isBatteryUnrestricted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                                val isOverlayGranted = Settings.canDrawOverlays(context)
                                
                                if (permissionsState.allPermissionsGranted && isBatteryUnrestricted && isOverlayGranted) {
                                    Graph.appPreferencesManager.setGatewayEnabled(true)
                                    val serviceIntent = android.content.Intent(context, com.enox.enoxpay.service.EnoxForegroundService::class.java)
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        context.startForegroundService(serviceIntent)
                                    } else {
                                        context.startService(serviceIntent)
                                    }
                                    com.enox.enoxpay.util.ToastManager.showToast("Gateway Service Started")
                                } else {
                                    com.enox.enoxpay.util.ToastManager.showToast("Please grant all permissions in Permission Center first", isError = true)
                                }
                            } else {
                                Graph.appPreferencesManager.setGatewayEnabled(false)
                                val serviceIntent = android.content.Intent(context, com.enox.enoxpay.service.EnoxForegroundService::class.java)
                                context.stopService(serviceIntent)
                                com.enox.enoxpay.util.ToastManager.showToast("Gateway Service Stopped", isError = true)
                            }
                        }
                    )
                }

                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "API Configuration",
                    subtitle = "Manage API endpoints & secrets",
                    onClick = { navController.navigate("api_config") }
                )
            }

            SettingsGroup(title = "Tools") {
                SettingsItem(
                    icon = Icons.Default.AddCircle,
                    title = "Manual Transaction",
                    subtitle = "Test parsing and push manually",
                    onClick = { navController.navigate("manual_transaction") }
                )
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Trash Bin",
                    subtitle = "Manage deleted platforms",
                    onClick = { navController.navigate("trash") }
                )
            }

            SettingsGroup(title = "System") {
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Permission Center",
                    subtitle = "Manage background & SMS access",
                    onClick = { navController.navigate("permissions") }
                )
                SettingsItem(
                    icon = Icons.Default.BatteryAlert,
                    title = "Battery & Background",
                    subtitle = "Ensure Gateway is never killed",
                    onClick = { navController.navigate("diagnostics") }
                )
            }
            
            SettingsGroup(title = "Developer") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Developer Docs",
                    subtitle = "A-Z Setup & Integration Guide",
                    onClick = { navController.navigate("developer_docs") }
                )
            }
            
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ENOX BD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    letterSpacing = 2.sp
                )
                Text(
                    text = "App Version: 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
