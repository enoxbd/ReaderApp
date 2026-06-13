package com.enox.enoxpay.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.enox.enoxpay.data.local.entity.SmsEntity
import com.enox.enoxpay.di.Graph
import com.enox.enoxpay.parser.SmsParserEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EnoxSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("EnoxSmsReceiver", "onReceive triggered with action: $action")
        if (action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isEmpty()) return
            Log.d("EnoxSmsReceiver", "Received SMS intent with ${messages.size} message(s)")
            
            val fullBody = java.lang.StringBuilder()
            var smsSender = ""
            var smsTimestamp = 0L
            
            for (sms in messages) {
                if (smsSender.isEmpty()) smsSender = sms.originatingAddress ?: ""
                if (smsTimestamp == 0L) smsTimestamp = sms.timestampMillis
                fullBody.append(sms.messageBody ?: "")
            }
            
            if (smsSender.isNotEmpty() && fullBody.isNotEmpty()) {
                Log.d("EnoxSmsReceiver", "Extracted Combined SMS: sender=$smsSender, timestamp=$smsTimestamp")
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        processSmsMessage(context, smsSender, fullBody.toString(), smsTimestamp)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        suspend fun processSmsMessage(context: Context, sender: String, body: String, timestamp: Long) {
            Log.d("EnoxSmsReceiver", "processSmsMessage called static: Sender=$sender, BodyLength=${body.length}")
            
            try {
                val gatewayEnabled = Graph.appPreferencesManager.isGatewayEnabled.value
                Log.d("EnoxSmsReceiver", "Gateway status: Enabled=$gatewayEnabled")
                if (!gatewayEnabled) {
                    Log.d("EnoxSmsReceiver", "Gateway disabled, ignoring message")
                    return
                }

                val repository = Graph.repository
                val platforms = repository.getEnabledPlatforms()
                val regexes = repository.getEnabledRegex()
                
                Log.d("EnoxSmsReceiver", "Loaded active data: Platforms=${platforms.size}, Regexes=${regexes.size}")
                val parser = SmsParserEngine(platforms, regexes)
                val result = parser.parse(sender, body)
                
                if (result != null) {
                    Log.d("EnoxSmsReceiver", "Matched transaction: TxID=${result.txId}, Platform=${result.platform}")
                    val entity = SmsEntity(
                        sender = result.sender,
                        messageBody = body,
                        timestamp = timestamp,
                        transactionId = if (result.txId.isEmpty()) null else result.txId,
                        amount = if (result.amount.isEmpty()) null else result.amount,
                        platformName = result.platform,
                        status = "PENDING"
                    )
                    val inserted = repository.insertSms(entity)
                    if (inserted) {
                        Log.d("EnoxSmsReceiver", "New SMS inserted, Enqueuing API worker")
                        // Enqueue WorkManager job to push to API
                        ApiWorker.enqueue(context)
                    } else {
                        Log.d("EnoxSmsReceiver", "Duplicate SMS ignored")
                    }
                } else {
                    Log.d("EnoxSmsReceiver", "Ignored message from sender=$sender (does not match active regex layout)")
                }
            } catch (e: Exception) {
                Log.e("EnoxSmsReceiver", "Error processing message", e)
            }
        }
    }
}
