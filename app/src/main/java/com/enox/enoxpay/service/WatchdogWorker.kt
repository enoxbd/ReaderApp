package com.enox.enoxpay.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class WatchdogWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        Log.d("WatchdogWorker", "Watchdog checking if service is running...")
        
        // If Gateway is disabled by the user, do not restart the service or process APIs here
        val isGatewayEnabled = com.enox.enoxpay.di.Graph.appPreferencesManager.isGatewayEnabled.value
        if (!isGatewayEnabled) {
            Log.d("WatchdogWorker", "Gateway is disabled, Watchdog skipping service restart.")
            return Result.success()
        }
        
        try {
            // Start the foreground service again if it was killed
            val serviceIntent = Intent(applicationContext, EnoxForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(serviceIntent)
            } else {
                applicationContext.startService(serviceIntent)
            }
            
            // Re-enqueue pending API hits
            ApiWorker.enqueue(applicationContext)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= 31 && e is android.app.ForegroundServiceStartNotAllowedException) {
                Log.w("WatchdogWorker", "Cannot start foreground service from background on Android 12+", e)
            } else {
                Log.w("WatchdogWorker", "Error starting service in Watchdog", e)
            }
        }
        
        return Result.success()
    }

    companion object {
        fun enqueuePeriodic(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "EnoxWatchdogWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
