# Backend integration contract

The Android app posts one JSON event to:

`POST /api/payment-monitor/events`

## Authentication

Require the dedicated device secret in `X-API-Key`. Compare secrets in constant time, keep the expected secret in server environment variables, and support immediate key rotation/revocation. Require HTTPS. Apply request-size limits and rate limits.

## Request

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

## Required server behavior

1. Validate authentication and JSON schema.
2. Accept only the configured DANA package name.
3. Enforce a maximum title/text length.
4. Insert the event using `event_id` as a unique idempotency key.
5. Return 2xx for a previously accepted duplicate, without processing it twice.
6. Store the event as `unverified` or `pending_review`.
7. Notify the admin Telegram chat that a DANA notification was observed.
8. Never mark an order paid or grant Premium solely because notification text contains an amount or the words "berhasil" / "diterima".

## Suggested event lifecycle

`received -> pending_verification -> verified | rejected`

Only an authoritative payment-provider callback, provider API reconciliation, or a documented manual admin verification should transition an order to `verified` and trigger Premium activation.

## Suggested database fields

- `event_id` (unique)
- `package_name`
- `title`
- `notification_text`
- `posted_at`
- `received_at`
- `status` (default `unverified`)
- `created_at`

Avoid storing full notification text longer than operationally necessary. It may contain personal or financial information.
