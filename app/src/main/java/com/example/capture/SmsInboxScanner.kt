package com.example.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.TransactionEntity
import com.example.data.TransactionSource
import com.example.domain.InrFormatter
import com.example.ml.DiasporaMlClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SmsInboxScanReport(
    val permissionGranted: Boolean,
    val inspectedCount: Int,
    val matchedCount: Int,
    val newlyInsertedCount: Int,
    val skippedCount: Int,
    val recentTraces: List<SmsParseTrace>,
    val scannedAtMillis: Long = System.currentTimeMillis(),
)

class SmsInboxScanner(
    private val context: Context,
    private val classifier: DiasporaMlClassifier,
) {
    fun hasReadSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun scanCurrentMonthInbox(
        monthKey: String = InrFormatter.currentMonthKey(),
    ): Pair<SmsInboxScanReport, List<TransactionEntity>> = withContext(Dispatchers.IO) {
        if (!hasReadSmsPermission()) {
            return@withContext SmsInboxScanReport(
                permissionGranted = false,
                inspectedCount = 0,
                matchedCount = 0,
                newlyInsertedCount = 0,
                skippedCount = 0,
                recentTraces = emptyList(),
            ) to emptyList()
        }

        val (startMillis, endMillis) = InrFormatter.monthBoundsMillis(monthKey)
        val parsedEntities = mutableListOf<TransactionEntity>()
        val traces = mutableListOf<SmsParseTrace>()
        var inspected = 0
        var matched = 0
        var skipped = 0

        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
        )
        val selection = "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} < ?"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())

        runCatching {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${Telephony.Sms.DATE} DESC",
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)

                while (cursor.moveToNext() && inspected < 500) {
                    inspected++
                    val sender = if (addressIdx >= 0) cursor.getString(addressIdx).orEmpty() else ""
                    val body = if (bodyIdx >= 0) cursor.getString(bodyIdx).orEmpty() else ""
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()

                    val trace = SmsTransactionParser.inspect(
                        rawMessage = body,
                        classifier = classifier,
                        timestampMillis = date,
                        sender = sender,
                    )

                    if (traces.size < 15) {
                        traces.add(trace)
                    }

                    if (trace.accepted && trace.amountInr != null && trace.merchant != null && trace.prediction != null) {
                        matched++
                        parsedEntities.add(
                            TransactionEntity(
                                amountInr = trace.amountInr,
                                merchant = trace.merchant,
                                rawSnippet = trace.sanitizedSnippet,
                                categoryName = trace.prediction.category.name,
                                timestamp = date,
                                sourceName = TransactionSource.SMS_INBOX.name,
                                mlConfidence = trace.prediction.confidence,
                                isUserCorrected = false,
                                dedupeHash = trace.dedupeHash ?: SmsTransactionParser.computeDedupeHash(
                                    trace.amountInr,
                                    trace.merchant,
                                    date,
                                ),
                            ),
                        )
                    } else {
                        skipped++
                    }
                }
            }
        }

        SmsInboxScanReport(
            permissionGranted = true,
            inspectedCount = inspected,
            matchedCount = matched,
            newlyInsertedCount = parsedEntities.size,
            skippedCount = skipped,
            recentTraces = traces,
        ) to parsedEntities
    }
}
