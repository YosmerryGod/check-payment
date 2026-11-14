package com.yosmerrygod.checkpayment

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object PaymentApi {
    data class Result(
        val httpStatus: Int,
        val status: String,
        val message: String,
        val shouldMarkSent: Boolean
    )

    fun send(context: Context, event: PaymentEvent): Result {
        val baseUrl = MonitorPrefs.baseUrl(context)
        val apiKey = MonitorPrefs.apiKey(context)

        if (!baseUrl.startsWith("https://", ignoreCase = true)) {
            return Result(0, "GAGAL", "Backend URL wajib menggunakan HTTPS", false)
        }
        if (apiKey.isBlank()) {
            return Result(0, "GAGAL", "API Key belum dikonfigurasi", false)
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

            val statusCode = connection.responseCode
            val responseBody = (if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            })?.bufferedReader()?.use { it.readText() }.orEmpty()

            val response = try {
                JSONObject(responseBody)
            } catch (_: Exception) {
                JSONObject()
            }

            when {
                statusCode == 200 && response.optBoolean("activated") -> {
                    Result(statusCode, "AKTIF", "Pembayaran cocok. Premium berhasil diaktifkan.", true)
                }
                statusCode == 200 && response.optBoolean("duplicate") -> {
                    Result(statusCode, "DUPLIKAT", "Event ini sudah pernah diproses backend.", true)
                }
                statusCode == 202 && response.optBoolean("matched").not() -> {
                    val reason = response.optString("reason", "NO_MATCHING_PENDING_ORDER")
                    Result(statusCode, "TIDAK COCOK", "Event diterima, tetapi order tidak ditemukan ($reason).", false)
                }
                statusCode in 200..299 -> {
                    Result(statusCode, "DITERIMA", "Backend menerima event, tetapi status aktivasi belum terkonfirmasi.", false)
                }
                else -> {
                    val error = response.optString("error").ifBlank { responseBody.take(180) }
                    Result(statusCode, "DITOLAK", "Backend menolak event: HTTP $statusCode $error", false)
                }
            }
        } catch (error: Exception) {
            Result(0, "GAGAL", error.message ?: "Gagal menghubungi backend", false)
        } finally {
            connection?.disconnect()
        }
    }
}
