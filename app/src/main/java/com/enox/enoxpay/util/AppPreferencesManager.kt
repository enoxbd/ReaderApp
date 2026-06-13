package com.enox.enoxpay.util

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("enox_app_prefs", Context.MODE_PRIVATE)
    
    private val _isGatewayEnabled = MutableStateFlow(prefs.getBoolean("gateway_enabled", false))
    val isGatewayEnabled: StateFlow<Boolean> = _isGatewayEnabled.asStateFlow()

    fun setGatewayEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("gateway_enabled", enabled) }
        _isGatewayEnabled.value = enabled
    }
}
