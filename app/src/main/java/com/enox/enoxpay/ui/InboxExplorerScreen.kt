package com.enox.enoxpay.ui

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.enox.enoxpay.di.Graph
import com.enox.enoxpay.parser.SmsParserEngine
import com.enox.enoxpay.service.EnoxSmsReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

data class InboxSms(val sender: String, val body: String, val timestamp: Long)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InboxExplorerScreen(navController: NavController = rememberNavController()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var smsList by remember { mutableStateOf<List<InboxSms>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val selectedMessages = remember { mutableStateListOf<InboxSms>() }

    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val list = mutableListOf<InboxSms>()
            val platforms = Graph.repository.getEnabledPlatforms()
            val regexes = Graph.repository.getEnabledRegex()
            
            try {
                val cursor = context.contentResolver.query(
                    Uri.parse("content://sms/inbox"),
                    arrayOf("address", "body", "date"),
                    null, null, "date DESC LIMIT 500"
                )
                cursor?.use {
                    val indexAddress = it.getColumnIndex("address")
                    val indexBody = it.getColumnIndex("body")
                    val indexDate = it.getColumnIndex("date")
                    
                    val keywordMatchers = platforms.map { it.detectionKeyword.replace(Regex("[^0-9a-zA-Z+]"), "") }.filter { it.isNotEmpty() }

                    while (it.moveToNext()) {
                        val sender = it.getString(indexAddress) ?: ""
                        val body = it.getString(indexBody) ?: ""
                        val cSender = sender.replace(Regex("[^0-9a-zA-Z+]"), "")
                        
                        var isMatch = false
                        for (kw in keywordMatchers) {
                            if (cSender.contains(kw, ignoreCase = true) || sender.contains(kw, ignoreCase = true)) {
                                isMatch = true
                                break
                            }
                        }
                        
                        if (isMatch) {
                            list.add(
                                InboxSms(
                                    sender = sender,
                                    body = body,
                                    timestamp = it.getLong(indexDate)
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Permissions might not be granted
            }
            smsList = list
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (selectedMessages.isEmpty()) "Inbox Explorer" else "${selectedMessages.size} Selected", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedMessages.isNotEmpty()) {
                        IconButton(onClick = {
                            val msgs = selectedMessages.toList()
                            selectedMessages.clear()
                            coroutineScope.launch {
                                for (msg in msgs) {
                                    EnoxSmsReceiver.processSmsMessage(context, msg.sender, msg.body, msg.timestamp)
                                }
                                com.enox.enoxpay.util.ToastManager.showToast("Pushed ${msgs.size} messages to server queue")
                            }
                        }) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Push Selected")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (smsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Matching SMS found with platform senders", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(smsList) { sms ->
                        val isSelected = selectedMessages.contains(sms)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .combinedClickable(
                                    onClick = {
                                        if (selectedMessages.isNotEmpty()) {
                                            if (isSelected) selectedMessages.remove(sms) else selectedMessages.add(sms)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelected) {
                                            selectedMessages.add(sms)
                                        }
                                    }
                                ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sms.sender, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(sms.body, style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }
    }
}
