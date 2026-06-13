package com.enox.enoxpay.parser

import android.util.Log
import com.enox.enoxpay.data.local.entity.PlatformEntity
import com.enox.enoxpay.data.local.entity.RegexEntity
import java.util.regex.Pattern

data class ParseResult(
    val amount: String,
    val sender: String,
    val txId: String,
    val platform: String,
    val method: String,
    val isRawMode: Boolean = false
)

class SmsParserEngine(
    private val enabledPlatforms: List<PlatformEntity>,
    private val enabledRegexes: List<RegexEntity>
) {
    fun parse(sender: String, messageBody: String): ParseResult? {
        Log.d("SmsParserEngine", "Parsing SMS from $sender")
        Log.d("SmsParserEngine", "Body: $messageBody")
        
        for (platform in enabledPlatforms) {
            val platformRules = enabledRegexes.filter { it.platformId == platform.id }
            val platformKeyword = platform.detectionKeyword.trim()
            val cleanSender = sender.replace(Regex("[^0-9+]"), "")
            
            // 1) Fallback check: If a platform has NO explicit regex rules configured
            if (platformRules.isEmpty()) {
                if (platformKeyword.isNotEmpty()) {
                    val cleanKeyword = platformKeyword.replace(Regex("[^0-9+]"), "")
                    val isTextMatch = sender.contains(platformKeyword, ignoreCase = true)
                    val isNumericMatch = cleanKeyword.isNotEmpty() && cleanSender.contains(cleanKeyword)
                    
                    if (isTextMatch || isNumericMatch) {
                        return ParseResult(
                            amount = "",
                            sender = sender,
                            txId = java.util.UUID.randomUUID().toString().substring(0, 8),
                            platform = platform.name,
                            method = "${platform.name} (Fallback)",
                            isRawMode = platform.isRawMode
                        )
                    }
                }
                continue // Try the next platform
            }

            // 2) Evaluate Regex Rules for this platform
            for (rule in platformRules) {
                try {
                    // Check if sender matches this rule or platform keyword
                    val senderPattern = rule.senderPattern.trim()
                    
                    val senderMatchesPattern = if (senderPattern.isNotEmpty()) {
                        try {
                            Pattern.compile(senderPattern, Pattern.CASE_INSENSITIVE).matcher(sender).find()
                        } catch (e: Exception) {
                            sender.contains(senderPattern, ignoreCase = true)
                        }
                    } else {
                        false
                    }

                    val senderMatchesKeyword = if (platformKeyword.isNotEmpty()) {
                        val cleanKeyword = platformKeyword.replace(Regex("[^0-9+]"), "")
                        sender.contains(platformKeyword, ignoreCase = true) || 
                            (cleanKeyword.isNotEmpty() && cleanSender.contains(cleanKeyword))
                    } else {
                        false
                    }

                    // A rule is only evaluated if the Sender matches EITHER the rule's Sender Pattern OR Platform's Detection Keyword.
                    // (If both are empty, we allow it to evaluate the body regexes)
                    if (senderPattern.isNotEmpty() || platformKeyword.isNotEmpty()) {
                        if (!senderMatchesPattern && !senderMatchesKeyword) {
                            continue // Sender didn't match, go to next rule
                        }
                    }

                    // Extract Data from Message Body
                    var extractedAmount = ""
                    var extractedTxId = ""
                    var extractedSender = sender
                    
                    var amountMatches = true // Defaults to true initially if the field is empty
                    val amountRegex = rule.amountRegex.trim()
                    if (amountRegex.isNotEmpty()) {
                        val matcher = Pattern.compile(amountRegex, Pattern.CASE_INSENSITIVE).matcher(messageBody)
                        if (matcher.find()) {
                            extractedAmount = if (matcher.groupCount() >= 1) {
                                matcher.group(1) ?: matcher.group(0) ?: ""
                            } else {
                                matcher.group(0) ?: ""
                            }
                        } else {
                            amountMatches = false // Regex provided but no match found in body
                        }
                    }

                    var txIdMatches = true // Defaults to true initially if the field is empty
                    val txIdRegex = rule.txIdRegex.trim()
                    if (txIdRegex.isNotEmpty()) {
                        val matcher = Pattern.compile(txIdRegex, Pattern.CASE_INSENSITIVE).matcher(messageBody)
                        if (matcher.find()) {
                            extractedTxId = if (matcher.groupCount() >= 1) {
                                matcher.group(1) ?: matcher.group(0) ?: ""
                            } else {
                                matcher.group(0) ?: ""
                            }
                        } else {
                            txIdMatches = false // Regex provided but no match found in body
                        }
                    }

                    val customSenderRegex = rule.senderRegex.trim()
                    if (customSenderRegex.isNotEmpty()) {
                        val matcher = Pattern.compile(customSenderRegex, Pattern.CASE_INSENSITIVE).matcher(messageBody)
                        if (matcher.find()) {
                            extractedSender = if (matcher.groupCount() >= 1) {
                                matcher.group(1) ?: matcher.group(0) ?: sender
                            } else {
                                matcher.group(0) ?: sender
                            }
                        }
                    }

                    // Final logic check: if all specified body regexes matched, consider it a success!
                    if (amountMatches && txIdMatches) {
                        return ParseResult(
                            amount = extractedAmount,
                            sender = extractedSender,
                            txId = extractedTxId.ifEmpty { java.util.UUID.randomUUID().toString().substring(0, 8) },
                            platform = platform.name,
                            method = "Regex Extraction",
                            isRawMode = platform.isRawMode
                        )
                    }

                } catch (e: Exception) {
                    Log.e("SmsParserEngine", "Regex syntax error in platform ${platform.name}: ${e.message}")
                }
            }
        }
        
        Log.d("SmsParserEngine", "No matching platform or rule found for SMS.")
        return null
    }
}
