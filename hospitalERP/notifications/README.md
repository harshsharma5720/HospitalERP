# notifications (`hospital-notifications`)

Delivery channels: email (SMTP) and SMS (Twilio), the message texts, and the **outbox** that keeps undelivered messages and retries them ([docs/RELIABLE_NOTIFICATIONS_PLAN.md](../../docs/RELIABLE_NOTIFICATIONS_PLAN.md)). It knows how to send a message, not *why* — it has no idea what an appointment or a leave is. Callers pass plain data.

**Depends on:** common · **Used by:** identity, appointments

## Public API — `com.itmonteur.hospitalerp.notifications`

| Class | Purpose |
|---|---|
| `NotificationService` | Appointment messages to patient and doctor by SMS + email: `appointmentBooked`, `appointmentCancelled`, `appointmentCancelledByDoctorLeave`, `appointmentReminder`. Takes a `NotificationService.AppointmentInfo` record (names, contacts, date, time). Runs `@Async`, so a slow mail server never blocks a request; failures are logged, never thrown. (Step C.2 moves these messages onto the outbox.) |
| `NotificationOutbox` | `queueEmail(to, subject, html, description, notAfter)`, `queueSms(phone, text, description, notAfter)`: stores the message in the **caller's transaction** (none if it rolls back; none without a recipient) and sends it right after the commit. Failed sends are retried after 1, 5, 15, 30, 60 minutes, then hourly — for at most 24 hours and never after `notAfter` (e.g. the appointment); the first attempt is always made. |
| `OutboxChannel`, `OutboxStatus` | `EMAIL` / `SMS`; `PENDING`, `SENDING` (claimed by a sender), `SENT`, `SKIPPED` (channel not configured), `FAILED` (retries ran out). |
| `SmsService` | Twilio SMS: `sendOtp`, `sendPasswordResetSms`, the appointment texts, `isEnabled()`, `mask(phone)` for logs. Without Twilio settings SMS is skipped with a warning. |
| `EmailService` | HTML emails (booking, cancellation, doctor notification, leave cancellation, reminder, password reset). User-supplied text is HTML-escaped. `isEnabled()` (an SMTP host is set), `layout(body)` (the common card + signature), `sendHtml(to, subject, html)` (a finished mail; used by the outbox). |

## Events

None published or consumed. (Appointments decides *when* to notify; see `AppointmentNotificationEvent` in the appointments module.)

## Internal — `notifications.internal`

`TwilioConfig`: initialises the Twilio client from the `twilio.*` properties.

Outbox: `OutboxMessage` (entity), `OutboxRepository` (due messages, `claim`, clean-up), `OutboxSender` (claims each due message for 10 minutes so two senders never send the same one, sends it outside any transaction, stores the result; a channel that isn't configured makes the message `SKIPPED`; deletes delivered / skipped messages after 30 days, failed ones after 90), `OutboxScheduler` (runs the sender after a commit that queued messages, every minute, and the clean-up nightly).

## Database

`notification_outbox` (`db/migration/notifications/V2026_10_07_1__notifications_outbox.sql`, in `app`): channel, recipient, subject, the rendered body, description, status, attempts, next attempt, give-up time, last error, created / sent time.

No REST endpoints.

## Configuration

| Env variable | Meaning |
|---|---|
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP server (optional; without it emails are skipped). |
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_TRIAL_NUMBER` | Twilio (optional; without it SMS are skipped). |

Outbox settings (`application.properties` keys, defaults fit production): `app.notifications.sender.enabled` (default `true`; tests switch the background sender off), `app.notifications.send-interval-ms` (default `60000`), `app.notifications.cleanup-cron` (default `0 30 3 * * *`).

## Tests

No unit tests of its own; in `app`: `NotificationOutboxTest` (queueing in the caller's transaction, delivery, the retry schedule, giving up, skipped channels, claims and a dead sender, clean-up) and `NotificationsH2Test` (appointment notifications only after the booking transaction commits).

Module diagram: [docs/modules/module-notifications.puml](../../docs/modules/module-notifications.puml).
