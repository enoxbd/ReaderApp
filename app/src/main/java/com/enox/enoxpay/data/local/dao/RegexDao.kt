package com.enox.enoxpay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.enox.enoxpay.data.local.entity.RegexEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegexDao {
    @Query("SELECT * FROM regex_rules")
    fun getAllRegexes(): Flow<List<RegexEntity>>

    @Query("SELECT * FROM regex_rules WHERE platformId = :platformId ORDER BY priority DESC")
    fun getRegexsForPlatform(platformId: Int): Flow<List<RegexEntity>>

    @Query("SELECT * FROM regex_rules WHERE isEnabled = 1 ORDER BY priority DESC")
    suspend fun getEnabledRegexsSuspend(): List<RegexEntity>

    @Query("SELECT * FROM regex_rules WHERE platformId = :platformId ORDER BY priority DESC")
    suspend fun getRegexsForPlatformSuspend(platformId: Int): List<RegexEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegex(regex: RegexEntity)

    @Update
    suspend fun updateRegex(regex: RegexEntity)

    @Query("DELETE FROM regex_rules WHERE id = :id")
    suspend fun deleteRegex(id: Int)
}
