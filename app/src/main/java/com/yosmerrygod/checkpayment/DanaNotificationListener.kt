package com.yosmerrygod.checkpayment

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class DanaNotificationListener : NotificationListenerService() {
    private val executor = Executors.newSingleThreadExecutor()

    // [ADDED] Match the confirmed incoming DANA Bisnis notification format.
    private val incomingPaymentPattern = Regex(
        """^Rp\\s?[\\d.]+(?:,\\d{2})?\\s+diterima DANA Bisnis\\.?$""",
        RegexOption.IGNORE_CASE
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != DANA_PACKAGE) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()

        // [ADDED] Ignore all DANA notifications except incoming-payment notifications.
        if (!title.equals(INCOMING_PAYMENT_TITLE, ignoreCase = true)) return

        // [MODIFIED] Inspect notification text fields separately and keep only an exact match.
        val bigText = extras.getCharSequence("android.bigText")?.toString().orEmpty()
        val body = extras.getCharSequence("android.text")?.toString().orEmpty()
        val lines = extras.getCharSequenceArray("android.textLines")
            ?.map { it.toString() }.orEmpty()
        val text = (listOf(bigText, body) + lines)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .firstOrNull { incomingPaymentPattern.matches(it) }
            ?: return

        val eventId = sha256("${sbn.packageName}| ${sbn.key}| ${sbn.postTime}|$title|$text")
        val event = PaymentEvent(
            eventId = eventId,
            packageName = sbn.packageName,
            title = title.take(MAX_TITLE_LENGTH),
            text = text.take(MAX_TEXT_LENGTH),
            postedAt = sbn.postTime,
            receivedAt = System.currentTimeMillis(),
            notificationKey = sbn.key
        )

        MonitorPrefs.setLastEvent(
            applicationContext,
            "${SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale("id", "ID")).format(Date())} — $title"
        )

        if (MonitorPrefs.wasSent(applicationContext, eventId)) {
            Log.d(TAG, "Duplicate notification ignored: $eventId")
            return
        }

        executor.execute {
            val result = PaymentApi.send(applicationContext, event)
            Log.i(TAG, "${result.message}; event=$eventId")
            if (result.success) {
                MonitorPrefs.markSent(applicationContext, eventId)
            }
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TAG = "CheckPayment"
        private const val DANA_PACKAGE = "id.dana"
        private const val INCOMING_PAYMENT_TITLE = "Pembayaran Masuk" // [ADDED]
        private const val MAX_TITLE_LENGTH = 200
        private const val MAX_TEXT_LENGTH = 2000
    }
}
