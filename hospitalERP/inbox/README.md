# inbox (`hospital-inbox`)

The notification centre ([docs/NOTIFICATION_CENTRE_PLAN.md](../../docs/NOTIFICATION_CENTRE_PLAN.md)): each user's messages in the app — a visit booked, moved, cancelled or due tomorrow; a bill made, paid, due for refund or refunded; a leave request to decide, or a leave approved or rejected. Read or unread; kept 90 days.

It writes messages for what other modules tell it through their events; none of them calls it.

**Depends on:** common, notifications (the appointment event's details), identity, staff, appointments, billing · **Used by:** app

## Public API — `com.itmonteur.hospitalerp.inbox`

| Class | Purpose |
|---|---|
| `Inbox` | `toUser(userId, kind, title, body, link)` (nothing for a null user, e.g. a walk-in without a login) and `toRole(role, …)` (every active account of the role). Written in the caller's transaction; a title longer than 120 or a text longer than 500 characters is cut ("…"), a link longer than 200 dropped. |
| `InboxKind` | What a message is about: `VISIT_BOOKED`, `VISIT_MOVED`, `VISIT_CANCELLED`, `VISIT_CANCELLED_BY_LEAVE`, `VISIT_REMINDER`, `BILL_READY`, `PAYMENT_RECEIVED`, `REFUND_DUE`, `REFUND_GIVEN`, `LEAVE_REQUESTED`, `LEAVE_APPROVED`, `LEAVE_REJECTED`. |

## Endpoints — `inbox.web`

`InboxController`, under `/api/inbox`, for every logged-in user and always their own messages (`InboxSecurityRules`): `GET` (the newest 50 of the last 90 days, newest first, and `unread`: how many of them all are unread), `GET /unread-count` (`{unread}`, asked every 30 seconds by the app), `POST /{id}/read` (another user's message → 404), `POST /read-all` (`{read}`: how many). A message: `id`, `kind`, `title`, `body`, `link` (a page of the app, or null), `createdAt`, `read`.

## Internal — `inbox.internal`

The listeners, each writing in the event's transaction: `VisitMessages` (`AppointmentNotificationEvent`: booked, moved and cancelled to the patient and the doctor; cancelled by the doctor's leave and the reminder to the patient; an event without recipients is skipped), `LeaveMessages` (`LeaveRequestedEvent` to every admin, `LeaveDecidedEvent` to the one who asked); `Texts` ("Tue 21 Oct", "10:00–10:30", "15–17 Oct"). `InboxStore` (adding, the newest, the unread count, marking read, deleting old ones), `InboxMessage` / `InboxMessageRepository`, `InboxMessageDTO`, `InboxDTO`, `InboxCleanup` (every night at 04:15, `app.inbox.cleanup-cron`: deletes messages older than 90 days).

## Database

Flyway `inbox/V2026_10_19_2__inbox_messages.sql`: `inbox_messages` (the user — deleted with the account —, the kind, title, text, link, when it was made and read; indexed by user and time).

## Tests

End-to-end in `app`: `InboxVisitsH2Test` (a visit booked, moved, reminded and cancelled — the patient and the doctor; a family member's visit to the account holder; walk-ins without a login: the doctor only; a leave request to the admins, its approval to the doctor and the cancelled patient, a rejection); `InboxH2Test` (each user's own messages, newest first; another user's can't be marked; one read, all read; anonymous refused; the newest 50, long texts cut, a long link dropped; 90 days: older ones not shown or counted, deleted at night; a role message to every active account of the role, not a deactivated one).
