package com.enox.enoxpay.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val isGatewayEnabled = com.enox.enoxpay.di.Graph.appPreferencesManager.isGatewayEnabled.value
            if (!isGatewayEnabled) {
                Log.d("BootReceiver", "Gateway is disabled. Not starting service on boot.")
                return
            }
            Log.d("BootReceiver", "Device rebooted. Resuming pending API pushes.")
            ApiWorker.enqueue(context)
            
            // Start the foreground service to ensure we keep listening for SMS
            val serviceIntent = Intent(context, EnoxForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
