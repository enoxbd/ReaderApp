package com.enox.enoxpay.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.enox.enoxpay.ui.viewmodel.MainViewModel
import com.enox.enoxpay.util.ExportUtil
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    val allSms by viewModel.allSms.collectAsState()
    val context = LocalContext.current
    var filterStatus by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    
    var selectedSms by remember { mutableStateOf<com.enox.enoxpay.data.local.entity.SmsEntity?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showDeleteSingleDialog by remember { mutableStateOf<com.enox.enoxpay.data.local.entity.SmsEntity?>(null) }

    val filteredSms = allSms.filter {
        val matchesStatus = if (filterStatus == "ALL") true else it.status == filterStatus
        val matchesSearch = if (searchQuery.isBlank()) true else {
            (it.transactionId?.contains(searchQuery, ignoreCase = true) == true) ||
            (it.platformName?.contains(searchQuery, ignoreCase = true) == true) ||
            (it.amount?.contains(searchQuery, ignoreCase = true) == true)
        }
        matchesStatus && matchesSearch
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("All Transactions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { ExportUtil.exportToCsv(context, filteredSms) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    IconButton(onClick = { showClearAllDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear All History", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background)) {

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Txn ID, amount, platform") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("ALL", "SUCCESS", "PENDING", "FAILED")) { status ->
                FilterChip(
                    selected = filterStatus == status,
                    onClick = { filterStatus = status },
                    label = { Text(status, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        if (filteredSms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSms) { sms ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                            .combinedClickable(
                                onClick = {
                                    selectedSms = sms
                                    showDetailsDialog = true
                                },
                                onLongClick = {
                                    showDeleteSingleDialog = sms
                                }
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        val platformInitial = sms.platformName?.firstOrNull()?.toString()?.uppercase() ?: "?"
                        val platformColor = when(sms.platformName?.lowercase()) {
                            "bkash" -> com.enox.enoxpay.ui.theme.BkashColor
                            "nagad" -> com.enox.enoxpay.ui.theme.NagadColor
                            "rocket" -> com.enox.enoxpay.ui.theme.RocketColor
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.size(52.dp).background(platformColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                                    Text(platformInitial, color = platformColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                                }
                                Column {
                                    Text(sms.platformName ?: "Unknown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    val date = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(sms.timestamp))
                                    Text("TXN: ${sms.transactionId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(max = 120.dp)) {
                                Text(
                                    text = "+৳ ${sms.amount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    color = if (sms.status == "SUCCESS") com.enox.enoxpay.ui.theme.SuccessGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val statusBgColor = if (sms.status == "SUCCESS") com.enox.enoxpay.ui.theme.SuccessBg else if (sms.status == "FAILED") MaterialTheme.colorScheme.errorContainer else com.enox.enoxpay.ui.theme.QueueBg
                                val statusTextColor = if (sms.status == "SUCCESS") com.enox.enoxpay.ui.theme.SuccessGreen else if (sms.status == "FAILED") MaterialTheme.colorScheme.error else com.enox.enoxpay.ui.theme.QueueOrange
                                Box(modifier = Modifier.background(statusBgColor, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                    Text(sms.status, style = MaterialTheme.typography.labelSmall, color = statusTextColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (showClearAllDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllDialog = false },
                title = { Text("Clear All History") },
                text = { Text("Are you sure you want to clear all transaction history? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearHistory()
                            showClearAllDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Clear All") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (showDeleteSingleDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteSingleDialog = null },
                title = { Text("Delete Transaction") },
                text = { Text("Are you sure you want to delete this transaction record? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSms(showDeleteSingleDialog!!.id)
                            showDeleteSingleDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteSingleDialog = null }) { Text("Cancel") }
                }
            )
        }

        if (showDetailsDialog && selectedSms != null) {
            AlertDialog(
                onDismissRequest = { showDetailsDialog = false },
                title = { Text("Log Details") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("TrxID: ${selectedSms?.transactionId}", fontWeight = FontWeight.Bold)
                        Text("Status: ${selectedSms?.status}", color = if (selectedSms?.status == "SUCCESS") com.enox.enoxpay.ui.theme.SuccessGreen else if (selectedSms?.status == "FAILED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text("HTTP Code: ${selectedSms?.httpStatusCode ?: "N/A"}", fontWeight = FontWeight.Bold)
                        Text("Server Response:", fontWeight = FontWeight.Bold)
                        Text(selectedSms?.serverResponse ?: "No response available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text("Raw Push Request Body:", fontWeight = FontWeight.Bold)
                        Text(selectedSms?.rawPushBody ?: "No raw body available", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text("Original SMS Sender: ${selectedSms?.sender}", fontWeight = FontWeight.Bold)
                        Text("Original SMS Body:", fontWeight = FontWeight.Bold)
                        Text(selectedSms?.messageBody ?: "", style = MaterialTheme.typography.bodySmall)

                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDetailsDialog = false }) { Text("Close") }
                }
            )
        }
    }
    }
}
