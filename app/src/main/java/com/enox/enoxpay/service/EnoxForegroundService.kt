package com.enox.enoxpay.service

import android.os.PowerManager
import android.app.Notification
import android.app.NotificationChannel
// ... existing imports ...
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.enox.enoxpay.MainActivity
import com.enox.enoxpay.R

class EnoxForegroundService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: ImageView? = null
    private var smsReceiver: EnoxSmsReceiver? = null
    private var networkCallback: android.net.ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                // 1073741824 is FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                startForeground(1, createNotification(), 1073741824)
            } else {
                startForeground(1, createNotification())
            }
        } catch (e: Exception) {
            android.util.Log.e("EnoxForegroundService", "Error starting foreground service", e)
        }
        setupFloatingIcon()
        registerSmsReceiver()
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val request = android.net.NetworkRequest.Builder()
                .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    android.util.Log.d("EnoxForegroundService", "Internet connection detected, instantly triggering sync")
                    ApiWorker.enqueue(applicationContext)
                }
            }
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            android.util.Log.e("EnoxForegroundService", "Error registering network callback", e)
        }
    }

    
    private fun registerSmsReceiver() {
        try {
            if (smsReceiver == null) {
                smsReceiver = EnoxSmsReceiver()
                val filter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
                filter.priority = 9999
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(smsReceiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    registerReceiver(smsReceiver, filter)
                }
                android.util.Log.d("EnoxForegroundService", "Successfully registered dynamic SMS broadcast receiver with export flag.")
            }
        } catch (e: Exception) {
            android.util.Log.e("EnoxForegroundService", "Failed to register receiver: ", e)
        }
    }

    private fun createNotification(): Notification {
        val channelId = "enox_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Enox Core Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Enox Pay Active")
            .setContentText("Listening for incoming transactions...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Placeholder
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun setupFloatingIcon() {
        if (!android.provider.Settings.canDrawOverlays(this)) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = ImageView(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            val dpToPx = 1 // Just 1 pixel
            layoutParams = android.view.ViewGroup.LayoutParams(dpToPx, dpToPx)
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isGatewayEnabled = com.enox.enoxpay.di.Graph.appPreferencesManager.isGatewayEnabled.value
        if (!isGatewayEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Return START_STICKY to ensure the service is restarted if it gets killed
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val isGatewayEnabled = com.enox.enoxpay.di.Graph.appPreferencesManager.isGatewayEnabled.value
        if (!isGatewayEnabled) return
        try {
            val restartServiceIntent = Intent(applicationContext, EnoxForegroundService::class.java).also {
                it.setPackage(packageName)
            }
            val restartServicePendingIntent: PendingIntent = PendingIntent.getService(
                this, 1, restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmService: android.app.AlarmManager =
                applicationContext.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmService.canScheduleExactAlarms()) {
                    alarmService.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        android.os.SystemClock.elapsedRealtime() + 1000,
                        restartServicePendingIntent
                    )
                } else {
                    alarmService.setAndAllowWhileIdle(
                        android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        android.os.SystemClock.elapsedRealtime() + 1000,
                        restartServicePendingIntent
                    )
                }
            } else {
                alarmService.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    android.os.SystemClock.elapsedRealtime() + 1000,
                    restartServicePendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
        try {
            smsReceiver?.let { unregisterReceiver(it) }
            smsReceiver = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            networkCallback?.let {
                val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                connectivityManager.unregisterNetworkCallback(it)
            }
            networkCallback = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
