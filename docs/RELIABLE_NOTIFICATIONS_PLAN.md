# Notifications that aren't lost — plan

**Branch:** `feature/reliable-notifications` (from `main` = ac64870)
**Why:** appointment emails and SMS (booked, cancelled, cancelled because of a doctor's leave, day-before reminder) are sent once, in a background thread. If the mail server or Twilio is down at that moment — or the app restarts before the thread runs — the message is lost; the error is only logged. The reminder is even marked as sent before anything went out.
**Idea list:** [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md#not-implemented-yet) — "not implemented yet" #5.

## Decisions (2026-10-07)

| Question | Decision |
|---|---|
| Approach | **An outbox, one row per message** (instead of Spring Modulith's event registry, which would replay a whole event and resend messages that already went out). Each email / SMS is saved in the same transaction as the booking or cancellation; a background sender delivers it and retries only what failed. |
| Retries | Growing gaps (1, 5, 15, 30, 60 minutes, then hourly), for **at most 24 hours and never after the appointment time** — then the message is marked failed. The first attempt is always made. |
| Visibility | **Admin → Notifications**: the messages that couldn't be delivered (to whom, what, last error) with a **Resend** button, and a count on the admin dashboard. |
| Retention | Delivered and skipped messages are **deleted after 30 days**, failed ones after 90 days (nightly job): the rows hold names, phone numbers and appointment times. |

## Scope

- **Covered:** the four appointment notifications — booked, cancelled, cancelled by a doctor's leave, day-before reminder (to patient and doctor, by email and SMS).
- **Not covered, on purpose:** login codes (OTP) and password-reset codes. They expire within minutes and the user is waiting for them, so they stay direct: sent right away, and an error is shown at once.

## Design

- **notifications module:** table `notification_outbox` (Flyway): channel (EMAIL / SMS), recipient, subject, the rendered body, a description (e.g. "booking SMS to patient"), status (PENDING, SENDING, SENT, SKIPPED, FAILED), attempts, next attempt, give-up time, last error, created / sent time. The message text is rendered when the row is written, so later template changes don't alter queued messages.
- **Sending:** right after the transaction commits (so messages still go out at once, as today) and every minute for what is due. A row is claimed before it is sent, so two senders never send the same row. A channel that isn't configured (no SMTP host, no Twilio account) marks the message **skipped**, not failed, so it isn't retried for nothing.
- **Callers:** `NotificationService` keeps its four methods, but they write outbox rows instead of sending; the appointments listener calls them inside the transaction, so a rolled-back change leaves no message.
- **Admin:** `GET /api/admin/notifications` (filter by status, paging) and `POST /api/admin/notifications/{id}/resend` in administration; the dashboard gets the number of undelivered messages.

## Steps

| Step | Work | Checked by |
|---|---|---|
| C.1 | **Outbox + sender** (notifications module): migration, entity, repository, `NotificationOutbox` (enqueue email / SMS), sender with claim, retries, give-up, skipped channels; nightly clean-up. No callers yet. | Tests: delivery, retry schedule, give-up, skip, claim, clean-up; MySQL migration test |
| C.2 | **Appointment notifications through the outbox:** email and SMS templates split into "render" and "send"; `NotificationService` enqueues; the listener runs in the transaction; the reminder flag is only set together with its messages. | End-to-end tests: booking → messages delivered; mail server down → retried, then delivered; rollback → no messages |
| C.3 | **Admin API:** list (status filter, paging), resend, undelivered count on the dashboard. | End-to-end test; endpoint and access snapshots updated on purpose |
| C.4 | **Frontend:** Admin → Notifications page (failed messages, Resend), sidebar link, dashboard tile. | Frontend tests + build |
| C.5 | **Docs:** module READMEs, README, idea list status, MySQL runbook. | — |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| C.0 Plan + decisions | ✅ done | 2026-10-07 | This file. |
| C.1 Outbox + sender | ✅ done | 2026-10-07 | Flyway `notifications/V2026_10_07_1__notifications_outbox.sql` (`body` as `text`, channel / status as varchar, index on status + next attempt). notifications module (now with JPA): public `NotificationOutbox` (`queueEmail`, `queueSms` — in the caller's transaction, nothing without a recipient, give-up = min(now + 24 h, `notAfter`), one send trigger per transaction after commit), `OutboxChannel`, `OutboxStatus`; internal `OutboxMessage` (state changes: sent, skipped, failed attempt with the gaps 1 / 5 / 15 / 30 / 60 min then hourly, resend), `OutboxRepository` (due ids, `claim` as a conditional UPDATE, clean-up), `OutboxSender` (claim for a 10-minute lease → send outside a transaction → store the result; channel not configured → `SKIPPED`), `OutboxScheduler` (after commit `@Async`, every minute, nightly clean-up; `app.notifications.sender.enabled=false` switches it off). `EmailService`: `isEnabled()` (SMTP host set), `layout()`, `sendHtml()`. No callers yet — appointment messages still go the old way until C.2. New `NotificationOutboxTest` (`@DataJpaTest`, mocked email / SMS, a clock moved by hand; 9 tests: delivery, the exact retry times 0 / 1 / 6 / 21 / 51 / 111 / 171 min and giving up, a retry that succeeds, the give-up rules incl. "first attempt always", skipped channels, two senders racing (and two claims: 1 then 0), a dead sender's lease running out, rollback / no recipient, clean-up). `DatabaseMigrationMySqlTest`: 7 migrations, the entity validates on MySQL; its context now switches the background sender off (it kept running after the container was gone and logged connection errors). MySQL runbook, README migrations table, notifications README. Checker OK, 130 backend tests (1 skipped). |
| C.2 Appointment notifications through the outbox | ✅ done | 2026-10-07 | New `AppointmentMessages` (notifications): the appointment texts — 5 emails (subject + full HTML in the common layout; the three that had their own copy of the layout now share it, wording unchanged) and 6 SMS — rendered when queued. `NotificationService` (`@Transactional`) queues instead of sending: booking 4 messages, cancellation 3, leave cancellation 2, reminder 2; retries end at the appointment's start (date + first time of "09:00 - 09:10"; end of the day without a time). `EmailService` / `SmsService` lost their appointment send methods (now transports + the direct password-reset / OTP messages). `AppointmentNotificationListener` is a plain `@EventListener`: it queues **inside** the booking / cancellation / reminder transaction, so the reminder flag and the reminder's messages are saved together. Test-first: `NotificationsH2Test` rewritten (sender switched off, email / SMS mocked): a booking queues exactly four messages (description, channel, recipient; give-up at the appointment or after 24 h), nothing sent before the sender runs, a refused second booking adds nothing, the sender delivers them with the right subjects / texts, cancelling queues three; a mail server that is down → pending with the error, retried, delivered; a rolled-back change queues nothing; the reminder flag is set with its messages, and only once. All four failed on the old code (nothing queued). READMEs (notifications, appointments). Checker OK, 132 backend tests (1 skipped). |
