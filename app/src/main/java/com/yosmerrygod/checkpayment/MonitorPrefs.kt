package com.yosmerrygod.checkpayment

import android.content.Context

object MonitorPrefs {
    private const val FILE_NAME = "check_payment_settings"
    private const val KEY_BASE_URL = "backend_base_url"
    private const val KEY_API_KEY = "backend_api_key"
    private const val KEY_LAST_EVENT = "last_event"
    private const val KEY_SENT_IDS = "sent_event_ids"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun baseUrl(context: Context): String =
        prefs(context).getString(KEY_BASE_URL, "").orEmpty()

    fun apiKey(context: Context): String =
        prefs(context).getString(KEY_API_KEY, "").orEmpty()

    fun saveConfig(context: Context, baseUrl: String, apiKey: String) {
        prefs(context).edit()
            .putString(KEY_BASE_URL, baseUrl.trim().trimEnd('/'))
            .putString(KEY_API_KEY, apiKey.trim())
            .apply()
    }

    fun lastEvent(context: Context): String =
        prefs(context).getString(KEY_LAST_EVENT, "Belum ada notifikasi DANA").orEmpty()

    fun setLastEvent(context: Context, value: String) {
        prefs(context).edit().putString(KEY_LAST_EVENT, value).apply()
    }

    @Synchronized
    fun wasSent(context: Context, eventId: String): Boolean =
        prefs(context).getStringSet(KEY_SENT_IDS, emptySet()).orEmpty().contains(eventId)

    @Synchronized
    fun markSent(context: Context, eventId: String) {
        val current = prefs(context).getStringSet(KEY_SENT_IDS, emptySet())
            .orEmpty().toMutableSet()
        current.add(eventId)
        while (current.size > 500) {
            current.remove(current.first())
        }
        prefs(context).edit().putStringSet(KEY_SENT_IDS, current).apply()
    }
}
