package com.enox.enoxpay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_queue")
data class SmsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val messageBody: String,
    val timestamp: Long,
    val transactionId: String?,
    val amount: String?,
    val platformName: String?,
    val status: String = "PENDING", // PENDING, SUCCESS, FAILED, IGNORED
    val rawPushBody: String? = null,
    val retryCount: Int = 0,
    val serverResponse: String? = null,
    val httpStatusCode: Int? = null
)
