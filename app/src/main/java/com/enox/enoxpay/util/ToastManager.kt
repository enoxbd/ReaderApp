package com.enox.enoxpay.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ToastMessage(val message: String, val isError: Boolean, val id: Long = System.currentTimeMillis())

object ToastManager {
    private val _toastFlow = MutableStateFlow<ToastMessage?>(null)
    val toastFlow: StateFlow<ToastMessage?> = _toastFlow.asStateFlow()

    fun showToast(message: String, isError: Boolean = false) {
        _toastFlow.value = ToastMessage(message, isError)
    }

    fun clearToast() {
        _toastFlow.value = null
    }
}
