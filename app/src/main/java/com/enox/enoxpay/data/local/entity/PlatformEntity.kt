package com.enox.enoxpay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "platforms")
data class PlatformEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val detectionKeyword: String,
    val apiEndpoint: String = "",
    val isEnabled: Boolean = true,
    val isTrashed: Boolean = false,
    val isRawMode: Boolean = false
)
