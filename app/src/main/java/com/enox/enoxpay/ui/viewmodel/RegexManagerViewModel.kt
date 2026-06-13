package com.enox.enoxpay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enox.enoxpay.data.local.entity.PlatformEntity
import com.enox.enoxpay.data.local.entity.RegexEntity
import com.enox.enoxpay.di.Graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RegexManagerViewModel : ViewModel() {
    private val repository = Graph.repository

    val platforms: StateFlow<List<PlatformEntity>> = repository.platforms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val regexes: StateFlow<List<RegexEntity>> = repository.allRegexes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun savePlatformWithRegex(platformId: Int, name: String, keyword: String, senderPattern: String, amountRegex: String, txIdRegex: String, senderRegex: String, isRawMode: Boolean) {
        viewModelScope.launch {
            val db = Graph.database
            val pIdToUse = if (platformId == 0) {
                // insert
                db.platformDao().insertPlatform(PlatformEntity(name = name, detectionKeyword = keyword, isRawMode = isRawMode)).toInt()
            } else {
                // update
                val existing = db.platformDao().getAllPlatformsSuspend().find { it.id == platformId }
                if (existing != null) {
                    db.platformDao().updatePlatform(existing.copy(name = name, detectionKeyword = keyword, isRawMode = isRawMode))
                    platformId
                } else {
                    db.platformDao().insertPlatform(PlatformEntity(id = platformId, name = name, detectionKeyword = keyword, isRawMode = isRawMode)).toInt()
                }
            }
            
            // Handle Regex
            val rules = db.regexDao().getRegexsForPlatformSuspend(pIdToUse)
            if (rules.isNotEmpty()) {
                db.regexDao().updateRegex(rules.first().copy(senderPattern = senderPattern, amountRegex = amountRegex, txIdRegex = txIdRegex, senderRegex = senderRegex))
            } else {
                db.regexDao().insertRegex(RegexEntity(platformId = pIdToUse, senderPattern = senderPattern, amountRegex = amountRegex, txIdRegex = txIdRegex, senderRegex = senderRegex))
            }
        }
    }

    fun trashPlatform(platform: PlatformEntity) {
        viewModelScope.launch {
            Graph.database.platformDao().updatePlatform(platform.copy(isTrashed = true))
        }
    }

    fun restorePlatform(platform: PlatformEntity) {
        viewModelScope.launch {
            Graph.database.platformDao().updatePlatform(platform.copy(isTrashed = false))
        }
    }

    fun deletePlatform(id: Int) {
        viewModelScope.launch {
            Graph.database.platformDao().deletePlatform(id)
        }
    }
    
    fun togglePlatform(platform: PlatformEntity) {
        viewModelScope.launch {
            Graph.database.platformDao().updatePlatform(platform.copy(isEnabled = !platform.isEnabled))
        }
    }

    fun exportConfig(): String {
        val array = JSONArray()
        platforms.value.forEach { p ->
            val r = regexes.value.find { it.platformId == p.id }
            val obj = JSONObject()
            obj.put("name", p.name)
            obj.put("detectionKeyword", p.detectionKeyword)
            obj.put("isRawMode", p.isRawMode)
            obj.put("isEnabled", p.isEnabled)
            
            if (r != null) {
                obj.put("senderPattern", r.senderPattern)
                obj.put("amountRegex", r.amountRegex)
                obj.put("txIdRegex", r.txIdRegex)
                obj.put("senderRegex", r.senderRegex)
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    fun importConfig(jsonString: String) {
        viewModelScope.launch {
            try {
                val array = JSONArray(jsonString)
                val db = Graph.database
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val name = obj.optString("name", "")
                    val keyword = obj.optString("detectionKeyword", "")
                    if (name.isEmpty() || keyword.isEmpty()) continue
                    
                    val isRawMode = obj.optBoolean("isRawMode", false)
                    val isEnabled = obj.optBoolean("isEnabled", true)
                    
                    val senderPattern = obj.optString("senderPattern", ".*")
                    val amountRegex = obj.optString("amountRegex", "")
                    val txIdRegex = obj.optString("txIdRegex", "")
                    val senderRegex = obj.optString("senderRegex", "")
                    
                    val pid = db.platformDao().insertPlatform(PlatformEntity(name = name, detectionKeyword = keyword, isRawMode = isRawMode, isEnabled = isEnabled)).toInt()
                    db.regexDao().insertRegex(RegexEntity(platformId = pid, senderPattern = senderPattern, amountRegex = amountRegex, txIdRegex = txIdRegex, senderRegex = senderRegex))
                }
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    com.enox.enoxpay.util.ToastManager.showToast("Imported \${array.length()} platforms successfully")
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    com.enox.enoxpay.util.ToastManager.showToast("Import failed: Invalid JSON data", isError = true)
                }
            }
        }
    }
}
