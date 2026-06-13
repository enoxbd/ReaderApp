package com.enox.enoxpay.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.enox.enoxpay.di.Graph
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class ApiWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = Graph.repository

        val pendingSmsFlow = repository.allSms.firstOrNull() ?: return Result.success()
        val pendingList = pendingSmsFlow.filter { (it.status == "PENDING" || it.status == "FAILED") && it.retryCount < 5 }
        
        var failureCount = 0

        for (sms in pendingList) {
            try {
                val config = repository.getConfigForPlatform(sms.platformName ?: "GLOBAL")
                if (config == null || config.apiUrl.isEmpty()) {
                    continue // cannot sync this one
                }

                // Formatting payload
                var jsonBody = config.bodyTemplate
                val isRawMode = sms.transactionId.isNullOrEmpty()
                
                jsonBody = jsonBody.replace("{platform}", if (isRawMode) "" else (sms.platformName ?: ""))
                jsonBody = jsonBody.replace("{amount}", if (isRawMode) "" else (sms.amount ?: ""))
                jsonBody = jsonBody.replace("{tx_id}", if (isRawMode) "" else (sms.transactionId ?: ""))
                jsonBody = jsonBody.replace("{sender}", if (isRawMode) "" else sms.sender)
                jsonBody = jsonBody.replace("{sms}", sms.messageBody.replace("\"", "\\\"").replace("\n", " "))
                jsonBody = jsonBody.replace("{time}", sms.timestamp.toString())

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toRequestBody(mediaType)
                
                val reqBuilder = Request.Builder()
                    .url(config.apiUrl)
                    .post(requestBody)
                
                if (config.authKey.isNotEmpty()) reqBuilder.addHeader("Authorization", config.authKey)
                if (config.bearerToken.isNotEmpty()) reqBuilder.addHeader("Authorization", "Bearer ${config.bearerToken}")
                // Custom headers could be processed here by splitting lines...
                
                val request = reqBuilder.build()
                val response = Graph.okHttpClient.newCall(request).execute()
                val responseBodyStr = response.body?.string() ?: ""
                
                if (response.isSuccessful) {
                    val updated = sms.copy(
                        status = "SUCCESS", 
                        rawPushBody = jsonBody,
                        serverResponse = responseBodyStr,
                        httpStatusCode = response.code
                    )
                    repository.updateSms(updated)
                    com.enox.enoxpay.util.NotificationHelper.showNotification(
                        applicationContext,
                        "Sync Success - ${sms.platformName}",
                        "TxID: ${sms.transactionId}\nAmt: ${sms.amount}\nSender: ${sms.sender}",
                        true
                    )
                } else {
                    val updated = sms.copy(
                        status = "FAILED", 
                        retryCount = sms.retryCount + 1, 
                        rawPushBody = jsonBody,
                        serverResponse = responseBodyStr,
                        httpStatusCode = response.code
                    )
                    repository.updateSms(updated)
                    failureCount++
                    com.enox.enoxpay.util.NotificationHelper.showNotification(
                        applicationContext,
                        "Sync Failed - ${sms.platformName}",
                        "Code: ${response.code}\nTxID: ${sms.transactionId}",
                        false
                    )
                }
            } catch (e: Exception) {
                Log.e("ApiWorker", "API push failed for txId ${sms.transactionId}", e)
                val updated = sms.copy(
                    status = "FAILED", 
                    retryCount = sms.retryCount + 1,
                    serverResponse = e.message
                )
                repository.updateSms(updated)
                failureCount++
                com.enox.enoxpay.util.NotificationHelper.showNotification(
                        applicationContext,
                        "Sync Error - ${sms.platformName}",
                        "Error: ${e.message}\nTxID: ${sms.transactionId}",
                        false
                    )
            }
        }
        
        return if (failureCount > 0) Result.retry() else Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
                
            val request = OneTimeWorkRequestBuilder<ApiWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.LINEAR,
                    java.time.Duration.ofSeconds(15) // Wait 15s before retry
                )
                .build()
                
            WorkManager.getInstance(context).enqueueUniqueWork("ApiSyncWork", androidx.work.ExistingWorkPolicy.REPLACE, request)
        }
    }
}

