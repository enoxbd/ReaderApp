package com.enox.enoxpay.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.enox.enoxpay.ui.theme.SuccessGreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

fun getAutoStartIntents(): List<Intent> {
    return listOf(
        Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
        Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
        Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
        Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
        Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
        Intent().setComponent(ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity"))
    )
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PermissionCenterScreen(navController: NavController = rememberNavController()) {
    val context = LocalContext.current
    val pm = context.packageManager
    
    val lifecycleOwner = LocalLifecycleOwner.current
    var lifecycleTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = buildList {
            add(android.Manifest.permission.RECEIVE_SMS)
            add(android.Manifest.permission.READ_SMS)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )

    val validAutoStartIntents = remember {
        getAutoStartIntents().filter { intent ->
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("System Permissions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(innerPadding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("ENOx Pay requires these to intercept and process payments in the background securely.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

        // SMS Permission
        PermissionCard(
            icon = Icons.Default.Email,
            title = "SMS Access",
            description = "Required to intercept incoming transaction SMS.",
            isGranted = permissionsState.allPermissionsGranted,
            onRequest = { permissionsState.launchMultiplePermissionRequest() }
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // Notifications Permission
        val notificationsState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
        } else null
        val isNotificationGranted = notificationsState?.status?.isGranted ?: true
        
        PermissionCard(
            icon = Icons.Default.Notifications,
            title = "Push Notifications",
            description = "Required to keep the gateway service alive.",
            isGranted = isNotificationGranted,
            onRequest = { notificationsState?.launchPermissionRequest() }
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // Overlay View Permission
        val isOverlayGranted = remember(lifecycleTrigger) { Settings.canDrawOverlays(context) }
        PermissionCard(
            icon = Icons.Default.Layers,
            title = "Floating Overlay",
            description = "Allows service persistence in background.",
            isGranted = isOverlayGranted,
            onRequest = { 
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:" + context.packageName)
                context.startActivity(intent)
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // Battery Optimization
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val isIgnoringBatteryOptimizations = remember(lifecycleTrigger) { powerManager.isIgnoringBatteryOptimizations(context.packageName) }
        PermissionCard(
            icon = Icons.Default.BatteryFull,
            title = "Unrestrict Battery",
            description = "Prevents Android from killing the background gateway.",
            isGranted = isIgnoringBatteryOptimizations,
            onRequest = {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:" + context.packageName)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(intent)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // Exact Alarms
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val isExactAlarmGranted = remember(lifecycleTrigger) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        }
        PermissionCard(
            icon = Icons.Default.Schedule,
            title = "Exact Alarms",
            description = "Required to instantly restart service if killed.",
            isGranted = isExactAlarmGranted,
            onRequest = {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    intent.data = Uri.parse("package:" + context.packageName)
                    context.startActivity(intent)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // Notification Listener Permission
        val isNotificationListenerGranted = remember(lifecycleTrigger) {
            val enabledListeners = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            enabledListeners?.contains(context.packageName) == true
        }
        PermissionCard(
            icon = Icons.Default.Notifications,
            title = "Notification Listener",
            description = "Intercepts transaction notifications (bKash/Nagad) if SMS is missed.",
            isGranted = isNotificationListenerGranted,
            onRequest = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                context.startActivity(intent)
            }
        )
        
        // Auto Start (If applicable)
        if (validAutoStartIntents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            PermissionCard(
                icon = Icons.Default.PlayArrow,
                title = "Auto Start Permission",
                description = "Required for your device brand to allow auto-boot.",
                isGranted = false, 
                grantButtonText = "Grant",
                onRequest = {
                    try {
                        context.startActivity(validAutoStartIntents.first())
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
                            context.startActivity(intent)
                        } catch (e2: Exception) {}
                    }
                }
            )
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
    }
}

@Composable
fun PermissionCard(icon: ImageVector, title: String, description: String, isGranted: Boolean, grantButtonText: String = "Grant", onRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).background(if (isGranted) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha=0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = if (isGranted) SuccessGreen else MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (isGranted) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = SuccessGreen, modifier = Modifier.size(32.dp))
            } else {
                Button(onClick = onRequest, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(12.dp)) {
                    Text(grantButtonText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
