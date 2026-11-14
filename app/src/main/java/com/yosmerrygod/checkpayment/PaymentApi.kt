package com.yosmerrygod.checkpayment

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL

object PaymentApi {
    data class Result(val success: Boolean, val message: String)

    fun send(context: Context, event: PaymentEvent): Result {
        val baseUrl = MonitorPrefs.baseUrl(context)
        val apiKey = MonitorPrefs.apiKey(context)

        if (!baseUrl.startsWith("https://", ignoreCase = true)) {
            return Result(false, "Backend URL wajib menggunakan HTTPS")
        }
        if (apiKey.isBlank()) {
            return Result(false, "API Key belum dikonfigurasi")
        }

        var connection: HttpURLConnection? = null
        return try {
            val endpoint = URL("${baseUrl.trimEnd('/')}/api/miniapp/payment-monitor/events")
            connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-API-Key", apiKey)
                setRequestProperty("Idempotency-Key", event.eventId)
            }

            connection.outputStream.use { output ->
                output.write(event.toJson().toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            if (status in 200..299) {
                Result(true, "Terkirim ke backend (HTTP $status)")
            } else {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?.take(300).orEmpty()
                Result(false, "Backend menolak event (HTTP $status) ${detail}")
            }
        } catch (error: Exception) {
            Result(false, error.message ?: "Gagal menghubungi backend")
        } finally {
            connection?.disconnect()
        }
    }
}
