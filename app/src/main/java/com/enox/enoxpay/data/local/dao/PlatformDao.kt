package com.enox.enoxpay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.enox.enoxpay.data.local.entity.PlatformEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlatformDao {
    @Query("SELECT * FROM platforms WHERE isTrashed = 0")
    fun getAllPlatforms(): Flow<List<PlatformEntity>>

    @Query("SELECT * FROM platforms WHERE isTrashed = 1")
    fun getTrashedPlatforms(): Flow<List<PlatformEntity>>

    @Query("SELECT * FROM platforms WHERE isEnabled = 1 AND isTrashed = 0")
    suspend fun getEnabledPlatformsSuspend(): List<PlatformEntity>

    @Query("SELECT * FROM platforms")
    suspend fun getAllPlatformsSuspend(): List<PlatformEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatform(platform: PlatformEntity): Long

    @Update
    suspend fun updatePlatform(platform: PlatformEntity)

    @Query("DELETE FROM platforms WHERE id = :id")
    suspend fun deletePlatform(id: Int)
}
