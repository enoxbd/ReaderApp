package com.enox.enoxpay.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enox.enoxpay.data.local.entity.PlatformEntity
import com.enox.enoxpay.data.local.entity.RegexEntity
import com.enox.enoxpay.ui.viewmodel.RegexManagerViewModel
import java.util.regex.Pattern
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegexManagerScreen(navController: NavController, viewModel: RegexManagerViewModel = viewModel()) {
    val context = LocalContext.current
    val platforms by viewModel.platforms.collectAsState()
    val regexes by viewModel.regexes.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingPlatform by remember { mutableStateOf<PlatformEntity?>(null) }
    var editingRegex by remember { mutableStateOf<RegexEntity?>(null) }
    
    var showMenu by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var trashConfirmPlatform by remember { mutableStateOf<PlatformEntity?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Platforms & Parsers", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export Configs (Copy)") },
                            onClick = {
                                showMenu = false
                                val json = viewModel.exportConfig()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Enox Configs", json))
                                com.enox.enoxpay.util.ToastManager.showToast("Configs copied to clipboard")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import Configs") },
                            onClick = {
                                showMenu = false
                                showImportDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPlatform = null
                    editingRegex = null
                    showDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 90.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Platform")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(innerPadding)) {

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(platforms) { platform ->
                    val pRegex = regexes.find { it.platformId == platform.id }
                    Card(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        colors = CardDefaults.cardColors(containerColor = if (platform.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = platform.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(platform.name.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Filter: ${platform.detectionKeyword}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        editingPlatform = platform
                                        editingRegex = pRegex
                                        showDialog = true
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Switch(
                                        checked = platform.isEnabled,
                                        onCheckedChange = { viewModel.togglePlatform(platform) },
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    IconButton(onClick = { trashConfirmPlatform = platform }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)

                            if (pRegex != null) {
                                RegexRuleRow("Amount Regex", pRegex.amountRegex)
                                RegexRuleRow("TxID Regex", pRegex.txIdRegex)
                                RegexRuleRow("Sender Body Extraction", pRegex.senderRegex)
                            } else {
                                Text("No Parser Logic Configured", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
    
    if (trashConfirmPlatform != null) {
        AlertDialog(
            onDismissRequest = { trashConfirmPlatform = null },
            title = { Text("Move to Trash") },
            text = { Text("Are you sure you want to move '${trashConfirmPlatform?.name}' to trash? You can restore it later.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.trashPlatform(trashConfirmPlatform!!)
                        com.enox.enoxpay.util.ToastManager.showToast("Platform Moved To Trash")
                        trashConfirmPlatform = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Move") }
            },
            dismissButton = {
                TextButton(onClick = { trashConfirmPlatform = null }) { Text("Cancel") }
            }
        )
    }

    if (showImportDialog) {
        var importJson by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Configs", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = importJson,
                    onValueChange = { importJson = it },
                    label = { Text("Paste JSON Configs") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 10
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (importJson.isNotBlank()) {
                        viewModel.importConfig(importJson)
                    }
                    showImportDialog = false
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDialog) {
        var formName by remember { mutableStateOf(editingPlatform?.name ?: "") }
        var formKeyword by remember { mutableStateOf(editingPlatform?.detectionKeyword ?: "") }
        var formIsRawMode by remember { mutableStateOf(editingPlatform?.isRawMode ?: false) }
        var formSenderFilter by remember { mutableStateOf(editingRegex?.senderPattern ?: ".*") }
        var formAmountRegex by remember { mutableStateOf(editingRegex?.amountRegex ?: "(?i)Amount:\\s*Tk\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)") }
        var formTxIdRegex by remember { mutableStateOf(editingRegex?.txIdRegex ?: "(?i)TxnID\\s*:?\\s*([A-Z0-9]+)") }
        var formSenderRegex by remember { mutableStateOf(editingRegex?.senderRegex ?: "(?i)Uddokta|Sender:\\s*(01\\d{9})") }

        // Live test fields
        var testMessage by remember { mutableStateOf("Cash In Received. Amount: Tk 305.00 Uddokta: 01717694001 TxnID: 75GJ5UGB Balance: 305.20 02/06/2026 02:07") }
        
        AlertDialog(
            onDismissRequest = { showDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.95f),
            title = { Text(if (editingPlatform == null) "New Config" else "Edit Config", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp)
                ) {
                    Text("Basic Info", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(value = formName, onValueChange = { formName = it }, label = { Text("Platform Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = formKeyword, onValueChange = { formKeyword = it }, label = { Text("Detection Keyword (e.g., bKash)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    
                    Text("Processing Mode", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(if (formIsRawMode) "Raw Mode" else "Parse Mode", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        androidx.compose.material3.Switch(checked = formIsRawMode, onCheckedChange = { formIsRawMode = it })
                    }
                    Text(if (formIsRawMode) "Raw Mode: Sends the entire SMS to the API endpoint without parsing transaction ID or amount. (tx_id, amount will be empty in payload)" else "Parse Mode: Uses regular expressions to extract Amount, TxID, and Sender from the SMS.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    if (!formIsRawMode) {
                        Text("Regex Rules (Group 1)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        
                        OutlinedTextField(value = formAmountRegex, onValueChange = { formAmountRegex = it }, label = { Text("Amount Regex") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = formTxIdRegex, onValueChange = { formTxIdRegex = it }, label = { Text("TxID Regex") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = formSenderRegex, onValueChange = { formSenderRegex = it }, label = { Text("Sender Regex (Optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Live Test", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(value = testMessage, onValueChange = { testMessage = it }, label = { Text("Sample SMS") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                                Text("Extracted Data:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Amount: ${extractRegex(formAmountRegex, testMessage)}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                                Text("TxID: ${extractRegex(formTxIdRegex, testMessage)}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                                Text("Sender: ${extractRegex(formSenderRegex, testMessage)}", style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (formName.trim().isEmpty() || formKeyword.trim().isEmpty()) {
                        com.enox.enoxpay.util.ToastManager.showToast("Name and Detection Keyword cannot be empty")
                        return@Button
                    }
                    viewModel.savePlatformWithRegex(
                        platformId = editingPlatform?.id ?: 0,
                        name = formName.trim(),
                        keyword = formKeyword.trim(),
                        senderPattern = formSenderFilter,
                        amountRegex = formAmountRegex,
                        txIdRegex = formTxIdRegex,
                        senderRegex = formSenderRegex,
                        isRawMode = formIsRawMode
                    )
                    com.enox.enoxpay.util.ToastManager.showToast("Platform Configuration Saved")
                    showDialog = false
                }) {
                    Text("Save Config")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RegexRuleRow(label: String, regex: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(regex.ifEmpty { "Not set" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
    }
}

fun extractRegex(regex: String, text: String): String {
    if (regex.isEmpty()) return "N/A"
    return try {
        val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) matcher.group(1) ?: "Found, but no group captured"
        else "No match"
    } catch (e: Exception) {
        "Invalid Regex"
    }
}
