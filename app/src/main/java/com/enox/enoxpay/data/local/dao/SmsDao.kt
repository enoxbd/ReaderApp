package com.enox.enoxpay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enox.enoxpay.data.local.entity.SmsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {
    @Query("SELECT * FROM sms_queue ORDER BY timestamp DESC")
    fun getAllSms(): Flow<List<SmsEntity>>

    @Query("SELECT * FROM sms_queue WHERE status = :status ORDER BY timestamp DESC")
    fun getSmsByStatus(status: String): Flow<List<SmsEntity>>

    @Query("SELECT * FROM sms_queue WHERE transactionId = :txId LIMIT 1")
    suspend fun getSmsByTransactionId(txId: String): SmsEntity?

    @Query("SELECT * FROM sms_queue WHERE messageBody = :body AND timestamp > :recentTimestamp LIMIT 1")
    suspend fun getRecentSmsByBody(body: String, recentTimestamp: Long): SmsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSms(sms: SmsEntity): Long

    @Update
    suspend fun updateSms(sms: SmsEntity)

    @Query("DELETE FROM sms_queue WHERE id = :id")
    suspend fun deleteSms(id: Long)

    @Query("DELETE FROM sms_queue")
    suspend fun clearHistory()
}
