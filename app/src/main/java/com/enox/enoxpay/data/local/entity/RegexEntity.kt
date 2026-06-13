package com.enox.enoxpay.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "regex_rules",
    foreignKeys = [
        ForeignKey(
            entity = PlatformEntity::class,
            parentColumns = ["id"],
            childColumns = ["platformId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("platformId")]
)
data class RegexEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val platformId: Int,
    val senderPattern: String,
    val amountRegex: String,
    val txIdRegex: String,
    val senderRegex: String,
    val isEnabled: Boolean = true,
    val priority: Int = 0
)
