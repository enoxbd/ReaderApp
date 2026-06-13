package com.enox.enoxpay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_config")
data class ApiConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val platformName: String = "GLOBAL", // e.g. "GLOBAL", "bKash", "Nagad", "Rocket"
    val apiUrl: String = "",
    val authKey: String = "",
    val secretToken: String = "",
    val bearerToken: String = "",
    val customHeader: String = "",
    val bodyTemplate: String = "{ \"platform\":\"{platform}\", \"amount\":\"{amount}\", \"transaction_id\":\"{tx_id}\", \"sender\":\"{sender}\", \"sms\":\"{sms}\", \"time\":\"{time}\" }"
)
