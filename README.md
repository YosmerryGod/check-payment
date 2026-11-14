# Check Payment

Android notification monitor for forwarding incoming DANA payment notifications to a configured backend (for example, WAGRAM).

## What this project does

- Uses Android's `NotificationListenerService` to observe notifications posted by the DANA app.
- Filters notifications by the DANA Android package (`id.dana`).
- Sends the notification title, text, package name, timestamp and a deterministic event ID to a configured HTTPS backend.
- Provides a small settings screen for the backend URL and API key.
- Keeps a local event history and prevents sending the same Android notification key more than once on the device.

## Important limitations

This app reads notification content only. It does **not** access a DANA account, query wallet balances, initiate transfers, or independently prove that funds settled. Notifications can be delayed, duplicated, edited, hidden, or spoofed. The backend must treat events as unverified signals and must not automatically grant paid access based only on notification text. Verify payments with an official payment provider or reconcile them manually before fulfilling an order.

The DANA package name and notification wording can change by app version/device. Test with real, low-risk transactions before production use. Android may stop background delivery due to battery restrictions or notification access being revoked.

## Build

1. Install Android Studio (Hedgehog or newer) with Android SDK 35.
2. Open this repository as a project.
3. Allow Gradle sync.
4. Build and install the `app` configuration on a physical Android phone.
5. Open Check Payment, enter the backend HTTPS URL and API key, and save.
6. Tap **Open notification access settings** and enable Check Payment.
7. Return to the app and confirm the access status is enabled.

A physical device with the DANA app installed is required for end-to-end testing. The Android emulator is not a substitute for a real DANA notification.

## Backend contract

The app sends an HTTP POST to:

```
{BACKEND_BASE_URL}/api/payment-monitor/events
```

Headers:

- `Content-Type: application/json`
- `X-API-Key: <configured key>`
- `Idempotency-Key: <event_id>`

Example body:

```json
{
  "event_id": "sha256-hex",
  "source": "android_notification",
  "package_name": "id.dana",
  "title": "Notifikasi DANA",
  "text": "Contoh isi notifikasi",
  "posted_at": 1790000000000,
  "received_at": 1790000001000,
  "notification_key": "opaque-android-notification-key"
}
```

Expected successful response: any HTTP 2xx. The backend should validate the API key, persist events idempotently, rate-limit requests, redact sensitive data, and place events into a **pending verification** state. Do not activate a WAGRAM Premium order from a parsed amount alone.

## Configuration

- Backend URL: your public HTTPS API base URL, without a trailing slash.
- API key: a dedicated random secret generated for this device. Do not commit it to Git.
- DANA package filter: currently `id.dana`.

## Privacy and security

Notification access is powerful: Android may expose notifications from other apps. This app only forwards notifications whose package name matches `id.dana`. Keep the API endpoint on HTTPS, use a unique revocable API key, restrict access on the server, and avoid sending OTPs, account credentials, or unrelated notification content.

## Project status

Initial implementation scaffold. Validate the DANA notification format, backend connectivity, battery behavior, and payment reconciliation before using it for live fulfillment.
