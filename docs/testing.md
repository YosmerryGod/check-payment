# Device test checklist

Use a dedicated test account and small test payments. Do not test against customer orders.

- [ ] Install the app on a physical Android device with DANA installed.
- [ ] Configure a valid HTTPS test endpoint and a device-specific API key.
- [ ] Grant notification access to Check Payment.
- [ ] Confirm the app shows notification access as active.
- [ ] Trigger a known DANA test notification.
- [ ] Confirm only notifications from package `id.dana` are forwarded.
- [ ] Confirm the server receives the expected JSON fields.
- [ ] Re-deliver/repost the same notification and confirm server idempotency.
- [ ] Disable internet temporarily and confirm failure is visible in Android logs.
- [ ] Re-enable internet and test a fresh notification.
- [ ] Test revoked notification access and Android battery restrictions.
- [ ] Confirm Telegram alerts go only to the configured admin chat.
- [ ] Confirm no order is fulfilled before authoritative verification.

## Inspect logs

Use Android Studio Logcat and filter by:

`CheckPayment`

The app logs backend HTTP status and event ID. Do not log API keys or full notification contents in production.
