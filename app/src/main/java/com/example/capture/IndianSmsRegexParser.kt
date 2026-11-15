package com.example.capture

import java.util.Locale

data class IndianRegexMatchResult(
    val isFinancialDebit: Boolean,
    val matchedPatternName: String,
    val matchedDebitKeyword: String?,
    val matchedCurrencySymbol: String?,
    val amountInr: Double?,
    val merchant: String?,
    val accountOrCardTail: String?,
    val referenceNumber: String?,
    val instrumentType: String,
    val rejectionReason: String? = null,
)

/**
 * Dedicated regex-based parser module engineered for Indian bank, credit card,
 * and NPCI UPI SMS alerts (HDFC, SBI, ICICI, Axis, Kotak, IDFC, PNB, BOB, GPay, PhonePe, Paytm).
 */
object IndianSmsRegexParser {

    // 1. Security / OTP / Non-Expense Exclusion Regexes
    private val otpExclusionRegex = Regex(
        """(?i)\b(otp|one[\s\-]?time\s+password|verification\s+code|auth\s+code|do\s+not\s+share|valid\s+for\s+\d+\s*min|mpin|cvv)\b""",
    )

    private val promoExclusionRegex = Regex(
        """(?i)\b(pre[\s\-]?approved|apply\s+now|cashback\s+offer|win\s+up\s+to|flat\s+\d+%\s+off|loan\s+of\s+(?:rs|inr|₹)|credit\s+limit\s+increase|upgrade\s+your\s+card)\b""",
    )

    private val failedTransactionRegex = Regex(
        """(?i)\b(failed|declined|unsuccessful|reversed|could\s+not\s+be\s+processed|cancelled|bounced|insufficient\s+funds|insufficient\s+balance)\b""",
    )

    private val upiLiteTopUpExclusionRegex = Regex(
        """(?i)\b(upi\s+lite\s+(?:top[\s\-]?up|load|loaded|added)|loaded\s+(?:to|into)\s+upi\s+lite)\b""",
    )

    // 2. Indian Debit & Spend Action Pattern
    private val indianDebitVerbRegex = Regex(
        """(?i)\b(debited|spent|paid|amt\s+sent|sent|withdrawn|deducted|charged|purchase\s+of|txn\s+of|payment\s+of|trf\s+to|transferred)\b""",
    )

    private val creditOnlyExclusionRegex = Regex(
        """(?i)\b(credited\s+to\s+your|deposited\s+in\s+your|received\s+in\s+your|salary\s+credited|refund\s+of|cashback\s+credited)\b""",
    )

    // 3. Currency + Indian Numbering Amount Regexes (e.g., INR 1,24,500.00 | Rs. 485 | ₹2,450.50)
    private val currencyAmountRegex = Regex(
        """(?i)(₹|rs\.?|inr)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
    )

    // SBI / PSU bank pattern where currency symbol is omitted after "debited by" or "debited for"
    private val bareDebitedAmountRegex = Regex(
        """(?i)\bdebited\s+(?:by|for|with)\s+(?:₹|rs\.?|inr)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
    )

    // 4. Named Indian Bank & UPI Merchant Extraction Regexes
    private data class NamedMerchantPattern(
        val patternName: String,
        val regex: Regex,
    )

    private val merchantPatterns = listOf(
        NamedMerchantPattern(
            patternName = "UPI_TRF_TO",
            regex = Regex("""(?i)\btrf\s+to\s+(?:vpa\s+)?([A-Za-z0-9@._&'\- ]{2,42}?)(?=\s+(?:ref|refno|utr|on|date|from|via|\.|;|$))"""),
        ),
        NamedMerchantPattern(
            patternName = "ICICI_SEMI_CREDITED",
            regex = Regex("""(?i);\s*([A-Za-z0-9@._&'\- ]{2,42}?)\s+credited"""),
        ),
        NamedMerchantPattern(
            patternName = "HDFC_SENT_TO",
            regex = Regex("""(?i)\b(?:paid|sent|transferred)\s+(?:to\s+)?(?:₹|rs\.?|inr)?[0-9,.]*\s*(?:from\s+.*?\s+)?to\s+([A-Za-z0-9@._&'\- ]{2,42}?)(?=\s+(?:on|via|using|for|ref|utr|date|\.|;|$))"""),
        ),
        NamedMerchantPattern(
            patternName = "PAYEE_TO_CLAUSE",
            regex = Regex("""(?i)\bto\s+(?:vpa\s+)?([A-Za-z0-9@._&'\- ]{2,42}?)(?=\s+(?:on|via|using|for|ref|refno|utr|date|from|\.|;|$))"""),
        ),
        NamedMerchantPattern(
            patternName = "CARD_SPENT_AT",
            regex = Regex("""(?i)\bat\s+([A-Za-z0-9@._&'\- ]{2,42}?)(?=\s+(?:on|via|using|for|ref|utr|avl|avbl|date|\.|;|$))"""),
        ),
        NamedMerchantPattern(
            patternName = "UPI_SLASH_HANDLE",
            regex = Regex("""(?i)\bupi[/:\-]([A-Za-z0-9@._&'\- ]{2,36}?)(?=[/:\-\s]|$)"""),
        ),
        NamedMerchantPattern(
            patternName = "NPCI_VPA_HANDLE",
            regex = Regex("""(?i)\bvpa\s+([A-Za-z0-9._\-]+@[A-Za-z0-9]+)"""),
        ),
    )

    // 5. Account / Card Tail & UPI Reference Regexes
    private val accountMaskRegex = Regex(
        """(?i)\b(?:a/c|acct|ac|card(?:\s+no\.?)?)\s*(?:no\.?\s*)?([*xX]+\d{3,4}|\d{4})\b""",
    )

    private val upiRefRegex = Regex(
        """(?i)\b(?:ref(?:\s*no\.?)?|utr|upi)\s*[:\-]?\s*(\d{6,14})\b""",
    )

    fun parse(rawMessage: String, senderAddress: String = ""): IndianRegexMatchResult {
        val normalized = rawMessage.trim().replace(Regex("\\s+"), " ")
        if (normalized.length < 10) {
            return IndianRegexMatchResult(
                isFinancialDebit = false,
                matchedPatternName = "NONE",
                matchedDebitKeyword = null,
                matchedCurrencySymbol = null,
                amountInr = null,
                merchant = null,
                accountOrCardTail = null,
                referenceNumber = null,
                instrumentType = "Unknown",
                rejectionReason = "Message too short to be a financial alert",
            )
        }

        if (otpExclusionRegex.containsMatchIn(normalized)) {
            return reject("OTP_FILTER", "OTP / security verification message")
        }
        if (promoExclusionRegex.containsMatchIn(normalized)) {
            return reject("PROMO_FILTER", "Promotional or marketing alert")
        }
        if (failedTransactionRegex.containsMatchIn(normalized)) {
            return reject("FAILED_TXN_FILTER", "Failed, declined, or reversed transaction")
        }
        if (upiLiteTopUpExclusionRegex.containsMatchIn(normalized)) {
            return reject("UPI_LITE_TOPUP_FILTER", "UPI Lite wallet top-up ignored to prevent double-counting")
        }

        val debitMatch = indianDebitVerbRegex.find(normalized)?.value
        val hasCreditOnly = creditOnlyExclusionRegex.containsMatchIn(normalized) && debitMatch == null
        if (hasCreditOnly) {
            return reject("CREDIT_INFLOW_FILTER", "Incoming credit / deposit alert (not an expense)")
        }
        if (debitMatch == null) {
            return reject("NO_DEBIT_VERB", "No spend or debit keyword ('Debited', 'Spent', 'Paid') detected")
        }

        val currencyMatch = currencyAmountRegex.find(normalized)
        val currencySymbol = currencyMatch?.groupValues?.getOrNull(1)?.uppercase(Locale.ENGLISH)
        val rawAmountStr = currencyMatch?.groupValues?.getOrNull(2)
            ?: bareDebitedAmountRegex.find(normalized)?.groupValues?.getOrNull(1)

        val amountInr = rawAmountStr?.replace(",", "")?.toDoubleOrNull()
        if (amountInr == null || amountInr <= 0.0 || amountInr > 5_00_00_000.0) {
            return reject("NO_INR_AMOUNT", "No valid ₹/INR/Rs. debit amount found")
        }

        var matchedRule = "SENDER_FALLBACK"
        var extractedMerchant: String? = null
        for (candidate in merchantPatterns) {
            val rawCandidate = candidate.regex.find(normalized)?.groupValues?.getOrNull(1)?.trim()
            if (!rawCandidate.isNullOrBlank()) {
                val cleaned = cleanMerchantName(rawCandidate)
                if (cleaned.length >= 2 && !isNoiseBankName(cleaned)) {
                    extractedMerchant = cleaned
                    matchedRule = candidate.patternName
                    break
                }
            }
        }

        if (extractedMerchant == null) {
            val cleanSender = senderAddress
                .replace(Regex("^[A-Z]{2}-"), "")
                .replace(Regex("[^A-Za-z0-9 ]"), " ")
                .trim()
            extractedMerchant = if (cleanSender.length >= 3) {
                cleanTitleCase(cleanSender)
            } else {
                "UPI / Card Merchant"
            }
        }

        val accountMask = accountMaskRegex.find(normalized)?.groupValues?.getOrNull(1)
        val refNo = upiRefRegex.find(normalized)?.groupValues?.getOrNull(1)
        val instrument = detectInstrument(normalized.lowercase(Locale.ENGLISH))

        return IndianRegexMatchResult(
            isFinancialDebit = true,
            matchedPatternName = matchedRule,
            matchedDebitKeyword = debitMatch,
            matchedCurrencySymbol = currencySymbol ?: "INR",
            amountInr = amountInr,
            merchant = extractedMerchant,
            accountOrCardTail = accountMask,
            referenceNumber = refNo,
            instrumentType = instrument,
            rejectionReason = null,
        )
    }

    private fun reject(rule: String, reason: String) = IndianRegexMatchResult(
        isFinancialDebit = false,
        matchedPatternName = rule,
        matchedDebitKeyword = null,
        matchedCurrencySymbol = null,
        amountInr = null,
        merchant = null,
        accountOrCardTail = null,
        referenceNumber = null,
        instrumentType = "Ignored",
        rejectionReason = reason,
    )

    private fun cleanMerchantName(raw: String): String {
        var name = raw
            .replace(Regex("@[A-Za-z0-9]+$"), "")
            .replace(Regex("""^(?:vpa|upi|mr|ms|m/s)\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+(?:pvt|ltd|private|limited|india)$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""[._]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (name.length > 32) {
            name = name.take(32).trim()
        }
        return cleanTitleCase(name)
    }

    private fun cleanTitleCase(value: String): String {
        return value.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                if (word.length <= 3 && word.all { it.isUpperCase() }) {
                    word
                } else {
                    word.lowercase(Locale.ENGLISH).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString()
                    }
                }
            }
    }

    private fun isNoiseBankName(name: String): Boolean {
        val lower = name.lowercase(Locale.ENGLISH)
        return lower in setOf(
            "hdfc bank", "icici bank", "sbi", "axis bank",
            "kotak bank", "your account", "card", "bank",
        )
    }

    private fun detectInstrument(lower: String): String = when {
        lower.contains("upi") || lower.contains("gpay") || lower.contains("google pay") ||
            lower.contains("phonepe") || lower.contains("paytm") || lower.contains("vpa") -> "UPI"
        lower.contains("card") || lower.contains("credit") || lower.contains("debit card") -> "Card"
        lower.contains("neft") || lower.contains("imps") || lower.contains("netbanking") -> "Bank Transfer"
        else -> "Bank A/c"
    }
}
