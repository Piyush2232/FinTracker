package com.example.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.FinTrackerApp
import com.example.data.TransactionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val pendingResult = goAsync()
        val app = context.applicationContext as? FinTrackerApp
        if (app == null) {
            pendingResult.finish()
            return
        }

        val combinedBySender = messages.groupBy { it.originatingAddress.orEmpty() }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for ((sender, parts) in combinedBySender) {
                    val body = parts.joinToString("") { it.messageBody.orEmpty() }
                    val timestamp = parts.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
                    app.repository.ingestIncomingMessage(
                        rawMessage = body,
                        sender = sender,
                        timestampMillis = timestamp,
                        source = TransactionSource.SMS_LIVE,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
