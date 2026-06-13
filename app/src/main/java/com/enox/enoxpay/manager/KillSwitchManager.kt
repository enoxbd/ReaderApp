package com.enox.enoxpay.manager

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.exitProcess
import java.security.MessageDigest

object KillSwitchManager {
    private const val PREFS_NAME = "EnoxKillSwitch"
    private const val KEY_STATUS = "app_status"

    private val _isAppKilled = MutableStateFlow(false)
    val isAppKilled: StateFlow<Boolean> = _isAppKilled

    private fun getSecureUrl(): String {
        val o = intArrayOf(109, 121, 121, 117, 120, 63, 52, 52, 106, 115, 116, 125, 103, 105, 51, 108, 110, 121, 109, 122, 103, 51, 110, 116, 52, 87, 106, 102, 105, 106, 119, 70, 117, 117, 52, 120, 121, 102, 121, 122, 120, 51, 111, 120, 116, 115)
        val sb = java.lang.StringBuilder()
        for (c in o) {
            sb.append((c - 5).toChar())
        }
        return sb.toString()
    }

    // Expected signature Hash (SHA-256) of YOUR app. 
    // To get the real one, you can log the hash of your current debug/release build.
    // Leaving it blank disables signature verification.
    private const val EXPECTED_SIGNATURE_HASH_SHA256 = "706C5B833DF40DC475CB8E6E65D02F8B8E40A7619848789C73A0DDDF4E4DC1BA" 

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isKilled = prefs.getBoolean(KEY_STATUS, false)
        _isAppKilled.value = isKilled
    }

    suspend fun checkStatus(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val url = URL(getSecureUrl())
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val status = jsonObject.optString("status")

                    if (status.lowercase() == "closed") {
                        executeKill(context)
                    } else if (status.lowercase() == "active") {
                        unKill(context)
                    }
                }
            } catch (e: Exception) {
                Log.e("KillSwitch", "Error checking status", e)
            }
        }
    }

    fun verifySignature(context: Context) {
        if (EXPECTED_SIGNATURE_HASH_SHA256.isEmpty()) return // disabled

        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners ?: emptyArray()
            } else {
                packageInfo.signatures ?: emptyArray()
            }

            for (signature in signatures) {
                val md = MessageDigest.getInstance("SHA-256")
                md.update(signature.toByteArray())
                val currentHash = md.digest().joinToString("") { "%02X".format(it) }

                if (currentHash.equals(EXPECTED_SIGNATURE_HASH_SHA256, ignoreCase = true)) {
                    return // Valid
                }
            }

            // If we loop through signatures and none map, then tampering detected
            // Only execute kill if signatures array is not empty
            if (signatures.isNotEmpty()) {
                executeKill(context)
                Log.e("KillSwitch", "App Tampering Detected! Signature mismatch.")
            }

        } catch (e: Exception) {
            Log.e("KillSwitch", "Failed to verify signature", e)
        }
    }

    private fun executeKill(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_STATUS, true).apply()
        _isAppKilled.value = true

        try {
            val intent = android.content.Intent(context, com.enox.enoxpay.service.EnoxForegroundService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {}
        
        // Disable Gateway Preferences globally via Graph (or trigger UI disable)
        try {
            com.enox.enoxpay.di.Graph.appPreferencesManager.setGatewayEnabled(false)
        } catch (e: Exception) {}
    }

    private fun unKill(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_STATUS, false).apply()
        _isAppKilled.value = false
    }
}
