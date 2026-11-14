package com.yosmerrygod.checkpayment

import org.json.JSONObject

data class PaymentEvent(
    val eventId: String,
    val packageName: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val receivedAt: Long,
    val notificationKey: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("event_id", eventId)
        put("source", "android_notification")
        put("package_name", packageName)
        put("title", title)
        put("text", text)
        put("posted_at", postedAt)
        put("received_at", receivedAt)
        put("notification_key", notificationKey)
    }
}
