package com.enox.enoxpay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enox.enoxpay.data.local.entity.ApiConfigEntity
import com.enox.enoxpay.data.local.entity.PlatformEntity
import com.enox.enoxpay.di.Graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ApiConfigViewModel : ViewModel() {
    private val repository = Graph.repository

    val allApiConfigs: StateFlow<List<ApiConfigEntity>> = repository.allApiConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val platforms: StateFlow<List<PlatformEntity>> = repository.platforms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveConfig(platformName: String, apiUrl: String, authKey: String, body: String) {
        viewModelScope.launch {
            val existing = allApiConfigs.value.find { it.platformName == platformName }
            val newConfig = existing?.copy(apiUrl = apiUrl, authKey = authKey, bodyTemplate = body) 
                ?: ApiConfigEntity(platformName = platformName, apiUrl = apiUrl, authKey = authKey, bodyTemplate = body)
            repository.apiConfigDao.insertApiConfig(newConfig)
        }
    }

    fun deleteConfig(configId: Int) {
        viewModelScope.launch {
            repository.apiConfigDao.deleteApiConfig(configId)
        }
    }
}

