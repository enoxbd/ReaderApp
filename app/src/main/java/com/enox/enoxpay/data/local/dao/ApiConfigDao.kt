package com.enox.enoxpay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enox.enoxpay.data.local.entity.ApiConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiConfigDao {
    @Query("SELECT * FROM api_config")
    fun getAllConfigs(): Flow<List<ApiConfigEntity>>

    @Query("SELECT * FROM api_config WHERE platformName = 'GLOBAL' LIMIT 1")
    fun getGlobalConfig(): Flow<ApiConfigEntity?>

    @Query("SELECT * FROM api_config WHERE platformName = :platform LIMIT 1")
    suspend fun getConfigForPlatform(platform: String): ApiConfigEntity?

    @Query("SELECT * FROM api_config WHERE platformName = 'GLOBAL' LIMIT 1")
    suspend fun getGlobalConfigSuspend(): ApiConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApiConfig(apiConfig: ApiConfigEntity)

    @Query("DELETE FROM api_config WHERE id = :id")
    suspend fun deleteApiConfig(id: Int)
}

