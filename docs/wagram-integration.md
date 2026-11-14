# WAGRAM integration plan

This repository contains the Android notification-monitor client. It does not modify the WAGRAM application or its database.

## Required WAGRAM backend work

Implement `POST /api/payment-monitor/events` using the contract in [backend-contract.md](backend-contract.md).

Recommended responsibilities:

- Authenticate the monitor device using a dedicated API key.
- Persist events in the existing WAGRAM database with a unique `event_id`.
- Send an admin-only Telegram Bot message when a new notification arrives.
- Show the event in an admin payment-monitor page or command.
- Match the event to a pending order only as a candidate match.
- Require provider reconciliation or explicit admin verification before setting an order to paid.
- Record the verification source, verifier, and timestamp in an audit trail.

## Why this is not an automatic DANA balance verifier

Android notification text is not a trusted payment-provider API. The notification may be delayed, duplicated, incomplete, or imitated. Amount matching is useful for surfacing a likely order, but it is not sufficient proof to deliver paid digital goods.

## Values to configure on Android

- Backend URL: public HTTPS base URL for WAGRAM.
- API Key: a unique secret generated for this monitor installation and stored as a server-side hash where practical.

Do not put Telegram bot tokens, database credentials, or the WAGRAM server's master secrets in the Android app.
