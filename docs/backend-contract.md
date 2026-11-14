# Backend integration contract

Check Payment posts matching DANA Business incoming-payment notifications to WAGRAM:

`POST /api/miniapp/payment-monitor/events`

## Authentication

Require the dedicated `X-API-Key` secret configured on the WAGRAM server as `DANA_PAYMENT_MONITOR_API_KEY`. Compare secrets in constant time, keep the expected secret in server environment variables, and require HTTPS. The same secret is entered in Check Payment.

## Request

The Android app only forwards notifications matching all of these conditions:

- Android package name is `id.dana`.
- Notification title is `Pembayaran Masuk`.
- Notification body matches the observed DANA Bisnis format: `Rp5.000 diterima DANA Bisnis.`.
- The amount is parsed as a positive integer number of Indonesian rupiah.
- Each notification has a SHA-256 event ID used for idempotency.

Example:

```json
{
  "event_id": "64-character-sha256-hex",
  "type": "incoming_payment",
  "source": "android_notification",
  "package_name": "id.dana",
  "title": "Pembayaran Masuk",
  "text": "Rp5.000 diterima DANA Bisnis.",
  "amount": 5000,
  "currency": "IDR",
  "posted_at": 1790000000000,
  "received_at": 1790000001000,
  "notification_key": "opaque-android-notification-key"
}
```

## WAGRAM automatic order matching

1. WAGRAM creates a dynamic QRIS order with a unique amount and a 15-minute expiry.
2. The backend validates the API key, DANA package, incoming-payment event type, title/body format, currency, positive integer amount, and event ID.
3. The backend atomically matches the amount to one unexpired `pending` Premium order.
4. A match marks that order `approved` and activates the mapped Premium plan. A duplicate event ID cannot activate it again.
5. An unmatched amount does not activate any account. The endpoint responds with `matched: false`.
6. The Mini App polls the order status endpoint and closes the QRIS dialog after showing the success check.

## Security limitation

This integration deliberately uses Android notification detection for automatic activation, but notification text is not a cryptographically signed payment-provider receipt. The API key authenticates the monitor installation, not the actual payment. A compromised device or leaked API key could forge an event. Unique QRIS amounts, 15-minute expiry, strict format checks, and idempotency reduce operational mistakes but do not provide the assurance of an official DANA/provider webhook or transaction-status API. Keep the key secret and use HTTPS.
