# WAGRAM integration

Check Payment sends incoming DANA Business notification events to the WAGRAM automatic Premium settlement endpoint.

## Endpoint

`POST /api/miniapp/payment-monitor/events`

Headers:

- `Content-Type: application/json`
- `X-API-Key: <DANA_PAYMENT_MONITOR_API_KEY>`
- `Idempotency-Key: <event_id>`

The WAGRAM server must have `DANA_PAYMENT_MONITOR_API_KEY` configured. Use the same long random secret in the Check Payment API Key field. The backend URL in the Android app is the public HTTPS origin only.

## Automatic flow

- WAGRAM creates a dynamic QRIS order with a unique IDR amount and 15-minute expiry.
- Check Payment only forwards the matching DANA Business incoming-payment notification format.
- WAGRAM matches the amount to an unexpired pending order and atomically activates the associated Premium membership.
- The WAGRAM Mini App polls `GET /api/miniapp/premium/orders/:orderId/status`; it shows a success check and returns to Chats when the order is approved.
- Event IDs are idempotent. An unmatched amount does not activate Premium.

## Important security limitation

The Android notification is not cryptographically signed by DANA. The API key authenticates the monitor installation, not the payment itself. This flow is suitable for controlled testing and lower-risk use, but it is weaker than an official DANA/provider webhook or transaction-status API. Keep the API key secret, use HTTPS, and monitor for unexpected activations.
