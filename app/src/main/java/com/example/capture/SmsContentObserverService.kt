package com.example.capture

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.FinTrackerApp
import com.example.data.TransactionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmsObserverStatus(
    val isServiceRunning: Boolean = false,
    val observedUri: String = "content://sms",
    val changeEventsCount: Int = 0,
    val lastDetectedSummary: String = "Waiting for incoming SMS changes...",
    val lastEventTimestamp: Long = 0L,
)

/**
 * Background Service that registers a ContentObserver on Telephony.Sms.CONTENT_URI
 * to monitor the Android SMS inbox in real time and automatically parse Indian
 * financial transactions via IndianSmsRegexParser + DiasporaMlClassifier.
 */
class SmsContentObserverService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var smsContentObserver: ContentObserver? = null

    @Volatile
    private var lastProcessedSmsTimestamp: Long = System.currentTimeMillis() - 60_000L

    override fun onCreate() {
        super.onCreate()
        registerSmsObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (smsContentObserver == null) {
            registerSmsObserver()
        }
        if (intent?.action == ACTION_SIMULATE_SMS_CHANGE) {
            val rawMessage = intent.getStringExtra(EXTRA_SIMULATED_BODY).orEmpty()
            val sender = intent.getStringExtra(EXTRA_SIMULATED_SENDER).orEmpty()
            if (rawMessage.isNotBlank()) {
                handleSimulatedIncomingSms(sender, rawMessage)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterSmsObserver()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun registerSmsObserver() {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onChange(selfChange, null)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                _observerStatus.update { current ->
                    current.copy(
                        isServiceRunning = true,
                        changeEventsCount = current.changeEventsCount + 1,
                        lastEventTimestamp = System.currentTimeMillis(),
                    )
                }
                inspectLatestInboxMessages()
            }
        }

        runCatching {
            contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                observer,
            )
            smsContentObserver = observer
            _observerStatus.update {
                it.copy(
                    isServiceRunning = true,
                    observedUri = Telephony.Sms.CONTENT_URI.toString(),
                )
            }
        }
    }

    private fun unregisterSmsObserver() {
        smsContentObserver?.let { observer ->
            runCatching { contentResolver.unregisterContentObserver(observer) }
        }
        smsContentObserver = null
        _observerStatus.update { it.copy(isServiceRunning = false) }
    }

    private fun inspectLatestInboxMessages() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val app = applicationContext as? FinTrackerApp ?: return
        serviceScope.launch {
            runCatching {
                val projection = arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                )
                val selection = "${Telephony.Sms.DATE} >= ?"
                val selectionArgs = arrayOf(lastProcessedSmsTimestamp.toString())

                contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    "${Telephony.Sms.DATE} DESC",
                )?.use { cursor ->
                    val addressIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
                    val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)

                    var count = 0
                    var newestTimestamp = lastProcessedSmsTimestamp
                    while (cursor.moveToNext() && count < 10) {
                        count++
                        val sender = if (addressIdx >= 0) cursor.getString(addressIdx).orEmpty() else ""
                        val body = if (bodyIdx >= 0) cursor.getString(bodyIdx).orEmpty() else ""
                        val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                        if (date > newestTimestamp) newestTimestamp = date

                        val trace = app.repository.ingestIncomingMessage(
                            rawMessage = body,
                            sender = sender,
                            timestampMillis = date,
                            source = TransactionSource.SMS_LIVE,
                        )
                        if (trace.accepted) {
                            _observerStatus.update { st ->
                                st.copy(
                                    lastDetectedSummary = "Parsed ₹${trace.amountInr?.toInt()} at ${trace.merchant} (${trace.matchedRegexRule})",
                                    lastEventTimestamp = System.currentTimeMillis(),
                                )
                            }
                        }
                    }
                    lastProcessedSmsTimestamp = newestTimestamp
                }
            }
        }
    }

    private fun handleSimulatedIncomingSms(sender: String, rawMessage: String) {
        val app = applicationContext as? FinTrackerApp ?: return
        serviceScope.launch {
            val now = System.currentTimeMillis()
            val trace = app.repository.ingestIncomingMessage(
                rawMessage = rawMessage,
                sender = sender,
                timestampMillis = now,
                source = TransactionSource.SMS_LIVE,
            )
            _observerStatus.update { current ->
                current.copy(
                    isServiceRunning = true,
                    changeEventsCount = current.changeEventsCount + 1,
                    lastDetectedSummary = if (trace.accepted) {
                        "Parsed ₹${trace.amountInr?.toInt()} at ${trace.merchant} [${trace.matchedRegexRule}]"
                    } else {
                        "Ignored (${trace.rejectionReason})"
                    },
                    lastEventTimestamp = now,
                )
            }
        }
    }

    companion object {
        const val ACTION_SIMULATE_SMS_CHANGE = "com.example.capture.ACTION_SIMULATE_SMS_CHANGE"
        const val EXTRA_SIMULATED_SENDER = "extra_simulated_sender"
        const val EXTRA_SIMULATED_BODY = "extra_simulated_body"

        private val _observerStatus = MutableStateFlow(SmsObserverStatus())
        val observerStatus: StateFlow<SmsObserverStatus> = _observerStatus.asStateFlow()

        fun start(context: Context) {
            runCatching {
                val intent = Intent(context, SmsContentObserverService::class.java)
                context.startService(intent)
            }
        }

        fun simulateIncomingSms(context: Context, sender: String, rawMessage: String) {
            runCatching {
                val intent = Intent(context, SmsContentObserverService::class.java).apply {
                    action = ACTION_SIMULATE_SMS_CHANGE
                    putExtra(EXTRA_SIMULATED_SENDER, sender)
                    putExtra(EXTRA_SIMULATED_BODY, rawMessage)
                }
                context.startService(intent)
            }
        }
    }
}
