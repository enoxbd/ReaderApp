package com.enox.enoxpay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enox.enoxpay.data.local.entity.ApiConfigEntity
import com.enox.enoxpay.data.local.entity.SmsEntity
import com.enox.enoxpay.di.Graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val repository = Graph.repository

    val allSms: StateFlow<List<SmsEntity>> = repository.allSms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apiConfig: StateFlow<ApiConfigEntity?> = repository.apiConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun deleteSms(id: Long) {
        viewModelScope.launch {
            repository.deleteSms(id)
            com.enox.enoxpay.util.ToastManager.showToast("Transaction deleted")
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearSmsHistory()
            com.enox.enoxpay.util.ToastManager.showToast("All history cleared")
        }
    }
}
