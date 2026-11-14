package com.yosmerrygod.checkpayment

import org.json.JSONObject

data class PaymentEvent(
    val eventId: String,
    val packageName: String,
    val title: String,
    val text: String,
    val amount: Long, // [ADDED] Amount in whole Indonesian rupiah.
    val postedAt: Long,
    val receivedAt: Long,
    val notificationKey: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("event_id", eventId)
        put("type", "incoming_payment") // [ADDED]
        put("source", "android_notification")
        put("package_name", packageName)
        put("title", title)
        put("text", text)
        put("amount", amount) // [ADDED]
        put("currency", "IDR") // [ADDED]
        put("posted_at", postedAt)
        put("received_at", receivedAt)
        put("notification_key", notificationKey)
    }
}
