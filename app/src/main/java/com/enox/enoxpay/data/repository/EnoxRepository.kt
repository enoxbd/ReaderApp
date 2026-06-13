package com.enox.enoxpay.data.repository

import com.enox.enoxpay.data.local.dao.ApiConfigDao
import com.enox.enoxpay.data.local.dao.PlatformDao
import com.enox.enoxpay.data.local.dao.RegexDao
import com.enox.enoxpay.data.local.dao.SmsDao
import com.enox.enoxpay.data.local.entity.ApiConfigEntity
import com.enox.enoxpay.data.local.entity.PlatformEntity
import com.enox.enoxpay.data.local.entity.RegexEntity
import com.enox.enoxpay.data.local.entity.SmsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EnoxRepository(
    private val platformDao: PlatformDao,
    private val regexDao: RegexDao,
    private val smsDao: SmsDao,
    val apiConfigDao: ApiConfigDao
) {
    private val insertMutex = Mutex()
    
    val platforms: Flow<List<PlatformEntity>> = platformDao.getAllPlatforms()
    val allRegexes: Flow<List<RegexEntity>> = regexDao.getAllRegexes()
    val allSms: Flow<List<SmsEntity>> = smsDao.getAllSms()
    val apiConfig: Flow<ApiConfigEntity?> = apiConfigDao.getGlobalConfig()
    val allApiConfigs: Flow<List<ApiConfigEntity>> = apiConfigDao.getAllConfigs()
    
    suspend fun getRegexForPlatform(platformId: Int): Flow<List<RegexEntity>> = regexDao.getRegexsForPlatform(platformId)

    suspend fun getPendingSmsByTxId(txId: String?): SmsEntity? {
        if (txId.isNullOrEmpty()) return null
        return smsDao.getSmsByTransactionId(txId)
    }

    suspend fun getRecentSmsByBody(sender: String, body: String, timestamp: Long): SmsEntity? {
        // Within the last 5 minutes to prevent duplicate double-processing
        return smsDao.getRecentSmsByBody(body, timestamp - 300000)
    }

    suspend fun insertSms(sms: SmsEntity): Boolean {
        insertMutex.withLock {
            val isGeneratedUuid = sms.transactionId?.length == 8 && sms.transactionId.matches(Regex("^[a-f0-9]{8}$"))
            
            if (!sms.transactionId.isNullOrEmpty() && !isGeneratedUuid) {
                // If it's a real TxID, guard against duplicate transaction ID unconditionally
                val existing = smsDao.getSmsByTransactionId(sms.transactionId)
                if (existing != null) return false
            }
            
            // Unconditionally guard against identical bodies within recent 5 min (handles the duplicate SMS action triggers properly)
            val existingByBody = getRecentSmsByBody(sms.sender, sms.messageBody, sms.timestamp)
            if (existingByBody != null) return false

            smsDao.insertSms(sms)
            return true
        }
    }
    
    suspend fun updateSms(sms: SmsEntity) {
        smsDao.updateSms(sms)
    }

    suspend fun deleteSms(id: Long) {
        smsDao.deleteSms(id)
    }

    suspend fun clearSmsHistory() {
        smsDao.clearHistory()
    }

    suspend fun getEnabledPlatforms() = platformDao.getEnabledPlatformsSuspend()
    suspend fun getEnabledRegex() = regexDao.getEnabledRegexsSuspend()

    suspend fun getConfigForPlatform(platformName: String): ApiConfigEntity? {
        val specific = apiConfigDao.getConfigForPlatform(platformName)
        if (specific != null) return specific
        return apiConfigDao.getGlobalConfigSuspend()
    }

    suspend fun initDefaults() {
        val existingPlatforms = platformDao.getEnabledPlatformsSuspend()
        if (existingPlatforms.isEmpty()) {
            val bkashId = platformDao.insertPlatform(PlatformEntity(name = "bKash", detectionKeyword = "bKash")).toInt()
            regexDao.insertRegex(RegexEntity(platformId = bkashId, senderPattern = "bKash", amountRegex = "(?i)Tk\\s*([\\d,]+\\.\\d+)", txIdRegex = "(?i)TrxID\\s*([A-Z0-9]+)", senderRegex = "(?i)from\\s*(01\\d+)"))
            
            val nagadId = platformDao.insertPlatform(PlatformEntity(name = "NAGAD", detectionKeyword = "NAGAD")).toInt()
            regexDao.insertRegex(RegexEntity(platformId = nagadId, senderPattern = "NAGAD", amountRegex = "(?i)Amount:\\s*(?:Tk\\.?)?\\s*([\\d,]+\\.\\d+)", txIdRegex = "(?i)TxnID\\s*:?\\s*([A-Z0-9]+)", senderRegex = "(?i)(?:Uddokta|Sender):\\s*(01\\d+)"))

            val rocketId = platformDao.insertPlatform(PlatformEntity(name = "Rocket", detectionKeyword = "Rocket")).toInt()
            regexDao.insertRegex(RegexEntity(platformId = rocketId, senderPattern = "Rocket", amountRegex = "(?i)Tk\\s*([\\d,]+\\.\\d+)", txIdRegex = "(?i)TxnId\\s*:?\\s*([A-Z0-9]+)", senderRegex = "(?i)A/C:\\s*([\\*A-Z0-9]+)"))
            
            if (apiConfigDao.getGlobalConfigSuspend() == null) {
                apiConfigDao.insertApiConfig(ApiConfigEntity(platformName = "GLOBAL"))
            }
        }
    }
}

