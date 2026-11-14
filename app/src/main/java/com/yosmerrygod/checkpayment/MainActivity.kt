package com.yosmerrygod.checkpayment

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var urlInput: EditText
    private lateinit var keyInput: EditText
    private lateinit var accessStatus: TextView
    private lateinit var lastEventStatus: TextView
    private lateinit var deliveryHistoryStatus: TextView // [ADDED]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        loadSettings()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::accessStatus.isInitialized) refreshStatus()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(TextView(this).apply {
            text = "Check Payment"
            textSize = 28f
            setTextColor(0xFF111827.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Pemantau notifikasi pembayaran DANA"
            textSize = 14f
            setPadding(0, dp(4), 0, dp(24))
        })

        root.addView(sectionTitle("Status monitoring"))
        accessStatus = TextView(this).apply { textSize = 16f }
        root.addView(accessStatus)

        val accessButton = Button(this).apply {
            text = "Open notification access settings"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        root.addView(accessButton)

        root.addView(sectionTitle("Backend WAGRAM"))
        urlInput = EditText(this).apply {
            hint = "https://api-domain-anda.com"
            textSize = 14f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        root.addView(urlInput)

        keyInput = EditText(this).apply {
            hint = "API Key"
            textSize = 14f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        root.addView(keyInput)

        root.addView(Button(this).apply {
            text = "Save configuration"
            setOnClickListener { saveSettings() }
        })

        root.addView(sectionTitle("Notifikasi terakhir"))
        lastEventStatus = TextView(this).apply {
            textSize = 14f
            setPadding(0, dp(4), 0, dp(12))
        }
        root.addView(lastEventStatus)

        // [ADDED] Show real backend delivery outcomes without requiring Android Logcat.
        root.addView(sectionTitle("Riwayat pengiriman ke WAGRAM"))
        deliveryHistoryStatus = TextView(this).apply {
            textSize = 13f
            setTextColor(0xFF374151.toInt())
            setPadding(0, dp(4), 0, dp(16))
        }
        root.addView(deliveryHistoryStatus)

        root.addView(TextView(this).apply {
            text = "Catatan: notifikasi hanya sinyal awal. Backend harus memverifikasi pembayaran sebelum mengaktifkan Premium."
            textSize = 12f
            setTextColor(0xFF6B7280.toInt())
        })

        setContentView(root)
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 17f
        setTextColor(0xFF111827.toInt())
        setPadding(0, dp(20), 0, dp(8))
    }

    private fun loadSettings() {
        urlInput.setText(MonitorPrefs.baseUrl(this))
        keyInput.setText(MonitorPrefs.apiKey(this))
    }

    private fun saveSettings() {
        val url = urlInput.text.toString().trim()
        val key = keyInput.text.toString().trim()
        if (!url.startsWith("https://", ignoreCase = true)) {
            Toast.makeText(this, "Gunakan URL HTTPS", Toast.LENGTH_LONG).show()
            return
        }
        if (key.isBlank()) {
            Toast.makeText(this, "API Key wajib diisi", Toast.LENGTH_LONG).show()
            return
        }
        MonitorPrefs.saveConfig(this, url, key)
        Toast.makeText(this, "Konfigurasi disimpan", Toast.LENGTH_SHORT).show()
    }

    private fun refreshStatus() {
        val enabled = isNotificationAccessEnabled()
        accessStatus.text = if (enabled) {
            "AKTIF — akses notifikasi diberikan"
        } else {
            "BELUM AKTIF — izinkan akses notifikasi"
        }
        lastEventStatus.text = MonitorPrefs.lastEvent(this)
        // [ADDED] Refresh persisted webhook results whenever the app opens or resumes.
        deliveryHistoryStatus.text = MonitorPrefs.deliveryHistory(this)
            .takeIf { it.isNotEmpty() }
            ?.joinToString("\n\n")
            ?: "Belum ada hasil pengiriman."
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val component = ComponentName(this, DanaNotificationListener::class.java)
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        return enabledListeners.split(":").any {
            ComponentName.unflattenFromString(it) == component
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
