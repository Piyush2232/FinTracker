package com.example.capture

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.FinTrackerApp
import com.example.data.TransactionSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaymentNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val pkg = sbn.packageName ?: return
        if (pkg !in SUPPORTED_PAYMENT_PACKAGES) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val combined = listOf(title, bigText.ifBlank { text }).filter { it.isNotBlank() }.joinToString(" ")
        if (combined.isBlank()) return

        val app = applicationContext as? FinTrackerApp ?: return
        serviceScope.launch {
            app.repository.ingestIncomingMessage(
                rawMessage = combined,
                sender = title,
                timestampMillis = sbn.postTime,
                source = TransactionSource.AUTO_NOTIFICATION,
            )
        }
    }

    companion object {
        private val SUPPORTED_PAYMENT_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user", // Google Pay India
            "com.phonepe.app",
            "net.one97.paytm",
            "in.org.npci.upiapp",
            "com.dreamplug.androidapp", // CRED
            "com.csam.icici.bank.imobile",
            "com.snapwork.hdfc",
            "com.sbi.lotusintouch",
            "com.axis.mobile",
            "com.msf.kbank.mobile",
        )

        fun isEnabled(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
            val componentName = ComponentName(context, PaymentNotificationListenerService::class.java)
            return enabledListeners.contains(componentName.flattenToString())
        }
    }
}
