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
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty().trim()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty().trim()
        val body = bigText.ifBlank { text }

        // Normalize title-first UPI notifications (e.g., Title="Zomato", Body="₹350 paid via UPI Lite")
        val combined = if (title.isNotBlank() && body.isNotBlank() &&
            !body.contains(" to ", ignoreCase = true) &&
            !body.contains(" at ", ignoreCase = true)
        ) {
            "$body to $title"
        } else {
            listOf(title, body).filter { it.isNotBlank() }.joinToString(" ")
        }
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
        val SUPPORTED_PAYMENT_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user", // Google Pay (Tez)
            "com.phonepe.app", // PhonePe
            "net.one97.paytm", // Paytm
            "in.org.npci.upiapp", // BHIM
            "com.dreamplug.androidapp", // CRED
            "in.amazon.mShop.android.shopping", // Amazon Pay
            "com.whatsapp", // WhatsApp Pay
            "money.super.app", // Super.money
            "com.naviapp", // Navi UPI
            "indwin.c3.shareapp", // Slice
            "money.jupiter", // Jupiter
            "com.epifi.paisa", // Fi Money
            "com.csam.icici.bank.imobile", // ICICI iMobile
            "com.snapwork.hdfc", // HDFC Bank
            "com.sbi.lotusintouch", // SBI YONO
            "com.axis.mobile", // Axis Mobile
            "com.msf.kbank.mobile", // Kotak 811
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
