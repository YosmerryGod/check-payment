# Backend integration contract

The Android app posts one JSON event to:

`POST /api/payment-monitor/events`

## Authentication

Require the dedicated device secret in `X-API-Key`. Compare secrets in constant time, keep the expected secret in server environment variables, and support immediate key rotation/revocation. Require HTTPS. Apply request-size limits and rate limits.

## Request

The Android app only forwards notifications matching all of these conditions:

- Android package name is `id.dana`.
- Notification title is `Pembayaran Masuk`.
- Notification body matches the observed DANA Bisnis format: `Rp5.000 diterima DANA Bisnis.`.
- The amount is parsed as an integer number of Indonesian rupiah.

Example:

```json
{
  "event_id": "sha256-hex",
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

## Required server behavior

1. Validate authentication and JSON schema.
2. Accept only the configured DANA package name.
3. Require `type=incoming_payment` and `currency=IDR`.
4. Validate that `amount` is a positive integer and enforce a maximum title/text length.
5. Insert the event using `event_id` as a unique idempotency key.
6. Return 2xx for a previously accepted duplicate, without processing it twice.
7. Store the event as `unverified` or `pending_review`.
8. Notify the admin Telegram chat that a DANA incoming-payment notification was observed.
9. Never mark an order paid or grant Premium solely because notification text contains an amount or the words "berhasil" / "diterima".

## Suggested event lifecycle

`received -> pending_verification -> verified | rejected`

Only an authoritative payment-provider callback, provider API reconciliation, or a documented manual admin verification should transition an order to `verified` and trigger Premium activation.

## Suggested database fields

- `event_id` (unique)
- `type`
- `amount`
- `currency`
- `package_name`
- `title`
- `notification_text`
- `posted_at`
- `received_at`
- `status` (default `unverified`)
- `created_at`

Avoid storing full notification text longer than operationally necessary. It may contain personal or financial information.
