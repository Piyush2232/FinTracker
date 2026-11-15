package com.example.capture

import com.example.domain.InrFormatter
import com.example.ml.DiasporaMlClassifier
import com.example.ml.MlPrediction
import java.security.MessageDigest
import java.util.Locale

data class SmsParseTrace(
    val accepted: Boolean,
    val statusTitle: String,
    val rejectionReason: String? = null,
    val amountInr: Double? = null,
    val merchant: String? = null,
    val instrument: String? = null,
    val matchedRegexRule: String? = null,
    val matchedDebitKeyword: String? = null,
    val accountTail: String? = null,
    val prediction: MlPrediction? = null,
    val sanitizedSnippet: String = "",
    val dedupeHash: String? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
)

data class SmsSamplePreset(
    val label: String,
    val sender: String = "AD-HDFCBK",
    val rawMessage: String,
    val isExpectedReject: Boolean = false,
)

object SmsTransactionParser {

    val testPresets: List<SmsSamplePreset> = listOf(
        SmsSamplePreset(
            label = "HDFC UPI · Swiggy",
            sender = "AD-HDFCBK",
            rawMessage = "Amt Sent Rs.485.00 From HDFC Bank A/C *1234 To SWIGGY ON 15-11-26 Ref 6319284712",
        ),
        SmsSamplePreset(
            label = "ICICI · Zudio",
            sender = "VM-ICICIB",
            rawMessage = "ICICI Bank Acct XX123 debited for Rs 1,899.00 on 15-Nov-26; ZUDIO RETAIL credited. UPI:431928",
        ),
        SmsSamplePreset(
            label = "SBI UPI · Blinkit",
            sender = "JD-SBIUPI",
            rawMessage = "Dear UPI user A/C X5421 debited by 640.50 on date 15Nov26 trf to BLINKIT GROCERIES Refno 43198234",
        ),
        SmsSamplePreset(
            label = "Axis Card · Chai Point",
            sender = "AX-AXISBK",
            rawMessage = "INR 280.00 spent on Axis Bank Card no. XX9012 at CHAI POINT on 15-11-26. Avl Limit: INR 1,42,000",
        ),
        SmsSamplePreset(
            label = "Kotak UPI · Uber India",
            sender = "VM-KOTAKB",
            rawMessage = "Rs.340.00 debited from Kotak Bank A/c XX8821 to VPA uberindia@axisbank on 15-11-26. UPI Ref 6319901122",
        ),
        SmsSamplePreset(
            label = "BESCOM · Electricity",
            sender = "AD-BESCOM",
            rawMessage = "INR 1,420.00 paid towards BESCOM ELECTRICITY BILL from A/c XX1234 via UPI Ref 6319445566",
        ),
        SmsSamplePreset(
            label = "Apollo Pharmacy",
            sender = "JD-HDFCBK",
            rawMessage = "Rs. 760.00 spent on HDFC Bank Card XX4410 at APOLLO PHARMACY on 15-Nov-26.",
        ),
        SmsSamplePreset(
            label = "Diaspora · Patel Bros",
            sender = "GPAY-ALERT",
            rawMessage = "Paid ₹2,450.00 to patelbrothers@okaxis via Google Pay for groceries",
        ),
        SmsSamplePreset(
            label = "OTP Alert (Should Reject)",
            sender = "AD-HDFCBK",
            rawMessage = "847291 is your OTP for Rs.485.00 transaction at Swiggy. Do not share with anyone.",
            isExpectedReject = true,
        ),
        SmsSamplePreset(
            label = "Promo Spam (Should Reject)",
            sender = "TD-LOANS",
            rawMessage = "Congrats! Pre-approved loan of Rs.5,00,000! Win cashback offer on UPI spends today.",
            isExpectedReject = true,
        ),
    )

    fun inspect(
        rawMessage: String,
        classifier: DiasporaMlClassifier,
        timestampMillis: Long = System.currentTimeMillis(),
        sender: String = "",
    ): SmsParseTrace {
        val normalized = rawMessage.trim().replace(Regex("\\s+"), " ")
        val regexResult = IndianSmsRegexParser.parse(normalized, sender)

        if (!regexResult.isFinancialDebit || regexResult.amountInr == null || regexResult.merchant == null) {
            return SmsParseTrace(
                accepted = false,
                statusTitle = "Safely Ignored",
                rejectionReason = regexResult.rejectionReason ?: "Did not match Indian financial debit pattern",
                matchedRegexRule = regexResult.matchedPatternName,
                sanitizedSnippet = sanitizeSnippet(normalized),
                timestampMillis = timestampMillis,
            )
        }

        val prediction = classifier.classify(regexResult.merchant, normalized)
        val dedupeHash = computeDedupeHash(regexResult.amountInr, regexResult.merchant, timestampMillis)

        return SmsParseTrace(
            accepted = true,
            statusTitle = "Transaction Parsed",
            rejectionReason = null,
            amountInr = regexResult.amountInr,
            merchant = regexResult.merchant,
            instrument = regexResult.instrumentType,
            matchedRegexRule = regexResult.matchedPatternName,
            matchedDebitKeyword = regexResult.matchedDebitKeyword,
            accountTail = regexResult.accountOrCardTail,
            prediction = prediction,
            sanitizedSnippet = sanitizeSnippet(normalized),
            dedupeHash = dedupeHash,
            timestampMillis = timestampMillis,
        )
    }

    private fun sanitizeSnippet(text: String): String {
        return text
            .replace(Regex("""\b\d{4}\s?\d{4}\s?\d{4}\b"""), "XXXX")
            .take(140)
    }

    fun computeDedupeHash(amountInr: Double, merchant: String, timestampMillis: Long): String {
        val threeMinuteBucket = timestampMillis / (3 * 60 * 1000L)
        val normMerchant = merchant.lowercase(Locale.ENGLISH).replace(Regex("[^a-z0-9]"), "").take(16)
        val formattedAmount = InrFormatter.format(amountInr, showDecimals = true)
        val input = "$formattedAmount|$normMerchant|$threeMinuteBucket"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(24)
    }
}
