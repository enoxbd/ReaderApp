package com.enox.enoxpay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.enox.enoxpay.data.local.entity.SmsEntity
import com.enox.enoxpay.data.local.entity.PlatformEntity
import com.enox.enoxpay.di.Graph
import com.enox.enoxpay.parser.SmsParserEngine
import kotlinx.coroutines.launch

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTransactionScreen(navController: NavController = rememberNavController()) {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var selectedTab by remember { mutableStateOf(0) }
    var resultText by remember { mutableStateOf("") }
    
    // Tab 0 State
    var rawSms by remember { mutableStateOf("") }
    var sender by remember { mutableStateOf("") }
    
    // Tab 1 State
    var manualTxId by remember { mutableStateOf("") }
    var manualAmount by remember { mutableStateOf("") }
    var manualSender by remember { mutableStateOf("") }
    var platforms by remember { mutableStateOf<List<PlatformEntity>>(emptyList()) }
    var selectedPlatform by remember { mutableStateOf<PlatformEntity?>(null) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val repo = Graph.repository
        platforms = repo.getEnabledPlatforms()
        if (platforms.isNotEmpty()) {
            selectedPlatform = platforms.first()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Manual Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(16.dp))

            TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Parse SMS", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Manual Entry", modifier = Modifier.padding(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedPlatform?.name ?: "Select Platform",
                    onValueChange = {},
                    label = { Text("Select Platform to Parse as") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    platforms.forEach { platform ->
                        DropdownMenuItem(
                            text = { Text(platform.name) },
                            onClick = {
                                selectedPlatform = platform
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = rawSms,
                onValueChange = { rawSms = it },
                label = { Text("Raw SMS Message Content") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        val repo = Graph.repository
                        val pList = repo.getEnabledPlatforms()
                        val rList = repo.getEnabledRegex()
                        val parser = SmsParserEngine(pList, rList)
                        
                        val senderInput = selectedPlatform?.detectionKeyword ?: ""
                        val parseResult = parser.parse(senderInput, rawSms)
                        if (parseResult != null) {
                            resultText = "Parsed Successfully!\nAmount: ${parseResult.amount}\nTxId: ${parseResult.txId}\nPlatform: ${parseResult.platform}\n\nSync queued."
                            repo.insertSms(
                                SmsEntity(
                                    sender = parseResult.sender,
                                    messageBody = rawSms,
                                    timestamp = System.currentTimeMillis(),
                                    transactionId = parseResult.txId,
                                    amount = parseResult.amount,
                                    platformName = parseResult.platform,
                                    status = "PENDING"
                                )
                            )
                            com.enox.enoxpay.service.ApiWorker.enqueue(context)
                            com.enox.enoxpay.util.ToastManager.showToast("Saved Manual SMS Transaction")
                        } else {
                            resultText = "Failed to parse SMS. Ensure the sender keyword and rules match."
                            com.enox.enoxpay.util.ToastManager.showToast("Could not parse SMS", isError = true)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Parse & Sync")
            }
        } else {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedPlatform?.name ?: "Select Platform",
                    onValueChange = {},
                    label = { Text("Platform") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    platforms.forEach { platform ->
                        DropdownMenuItem(
                            text = { Text(platform.name) },
                            onClick = {
                                selectedPlatform = platform
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = manualTxId,
                onValueChange = { manualTxId = it },
                label = { Text("Transaction ID (TxID)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = manualAmount,
                onValueChange = { manualAmount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = manualSender,
                onValueChange = { manualSender = it },
                label = { Text("Sender Account (e.g. 017XXXXXXX)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (selectedPlatform != null && manualTxId.isNotEmpty() && manualAmount.isNotEmpty()) {
                        coroutineScope.launch {
                            Graph.repository.insertSms(
                                SmsEntity(
                                    sender = manualSender,
                                    messageBody = "Manual Entry",
                                    timestamp = System.currentTimeMillis(),
                                    transactionId = manualTxId,
                                    amount = manualAmount,
                                    platformName = selectedPlatform!!.name,
                                    status = "PENDING"
                                )
                            )
                            resultText = "Transaction saved manually!\nSync queued."
                            com.enox.enoxpay.service.ApiWorker.enqueue(context)
                            com.enox.enoxpay.util.ToastManager.showToast("Manual Entry Saved & Sync Queued")
                            manualTxId = ""
                            manualAmount = ""
                            manualSender = ""
                        }
                    } else {
                        resultText = "Please fill in all required fields."
                        com.enox.enoxpay.util.ToastManager.showToast("Please fill in all required fields", isError = true)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Sync")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (resultText.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(resultText, modifier = Modifier.padding(16.dp))
            }
        }
    }
    }
}

