package com.enox.enoxpay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.enox.enoxpay.ui.EnoxApp
import com.enox.enoxpay.ui.theme.MyApplicationTheme

import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.isSystemInDarkTheme
import com.enox.enoxpay.di.Graph
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import com.enox.enoxpay.manager.KillSwitchManager
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Initialize & Execute Kill Switch / Anti-Tamper checks
    KillSwitchManager.init(this)
    KillSwitchManager.verifySignature(this)
    
    lifecycleScope.launch {
        KillSwitchManager.checkStatus(this@MainActivity)
    }

    setContent {
      val isAppKilled by KillSwitchManager.isAppKilled.collectAsState()
      
      if (isAppKilled) {
          // If killed, show full screen red block and kill process if they minimize
          Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFB00020)) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text(
                      text = "Enox Pay Service is Closed by Administrator\nAccess Denied.",
                      color = Color.White,
                      style = MaterialTheme.typography.headlineMedium,
                      textAlign = TextAlign.Center,
                      modifier = Modifier.padding(16.dp)
                  )
              }
          }
      } else {
          val themeMode by Graph.themeManager.themeMode.collectAsState()
          val isDark = when(themeMode) {
              1 -> true
              else -> false
          }
          MyApplicationTheme(darkTheme = isDark) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
              EnoxApp()
            }
          }
      }
    }
  }
}

