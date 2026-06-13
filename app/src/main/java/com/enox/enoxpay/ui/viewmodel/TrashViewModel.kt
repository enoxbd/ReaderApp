package com.enox.enoxpay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enox.enoxpay.data.local.entity.PlatformEntity
import com.enox.enoxpay.di.Graph
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel : ViewModel() {
    val trashedPlatforms: StateFlow<List<PlatformEntity>> = Graph.database.platformDao().getTrashedPlatforms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restorePlatform(platform: PlatformEntity) {
        viewModelScope.launch {
            Graph.database.platformDao().updatePlatform(platform.copy(isTrashed = false))
        }
    }

    fun deletePlatformPermanently(id: Int) {
        viewModelScope.launch {
            Graph.database.platformDao().deletePlatform(id)
        }
    }
}
