package com.enox.enoxpay.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.enox.enoxpay.data.local.entity.SmsEntity
import java.io.File
import java.io.FileWriter

object ExportUtil {
    fun exportToCsv(context: Context, smsList: List<SmsEntity>) {
        try {
            val fileName = "enox_transactions_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)
            
            // Write Header
            writer.append("ID,Sender,Body,Amount,Transaction ID,Status,Time\n")
            
            // Write Data
            smsList.forEach { sms ->
                writer.append("${sms.id},")
                writer.append("\"${sms.sender.replace("\"", "\"\"")}\",")
                writer.append("\"${sms.messageBody.replace("\"", "\"\"").replace("\n", " ")}\",")
                writer.append("${sms.amount ?: ""},")
                writer.append("${sms.transactionId ?: ""},")
                writer.append("${sms.status},")
                writer.append("${sms.timestamp}\n")
            }
            
            writer.flush()
            writer.close()
            
            // Share
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "text/csv"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Transactions"))
            
        } catch (e: Exception) {
            e.printStackTrace()
            ToastManager.showToast("Export failed: ${e.message}", isError = true)
        }
    }
}
