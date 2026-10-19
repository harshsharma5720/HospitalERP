# Notification centre — plan

Today the app tells people what happened only by email and SMS, and only for visits: booked, cancelled (also by a doctor's leave), and the reminder the day before. Without Twilio or a mail server nothing arrives at all. Moving a visit, a bill ready at check-in, a payment, a refund, a leave decision or a leave request to decide reach nobody unless they look. This is idea #6 in [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md): a bell in the app with each user's messages.

## Decisions (2026-10-19)

| Question | Decision |
|---|---|
| Who | **Everyone, each with their own:** patients their visits and bills; doctors their bookings, cancellations and leave decisions; the front desk and admins what needs them. |
| What | **Visits** (booked, moved, cancelled — also by a doctor's leave —, the reminder), **bills and payments** (a bill made at check-in, a payment received, a refund due or given), **leave** (a doctor's leave approved or rejected; a new request for admins). Problems for admins (undelivered messages, a day closed short) are left out. |
| Updates | **Within 30 seconds:** the app asks for new ones every 30 seconds and when the tab is opened again. |
| Way of working | Each finished, tested step is committed without asking; the branch is pushed at the end with the PR link. |

## Design

**Who gets what** (one message per person; the same words as the SMS / email where those exist):

| Event | Patient (the account holder; a family member's visits too) | Doctor | Front desk | Admins |
|---|---|---|---|---|
| Visit booked (online, at the desk, walk-in) | ✓ | ✓ | | |
| Visit moved | ✓ | ✓ | | |
| Visit cancelled (by the patient, the desk, a deactivation) | ✓ | ✓ | | |
| Visit cancelled by the doctor's leave | ✓ | | | |
| Reminder the day before | ✓ | | | |
| Bill made at check-in | ✓ | | | |
| Payment received (at the desk or online) | ✓ | | | |
| Refund due (a paid visit cancelled) | ✓ | | ✓ (every receptionist) | |
| Refund given | ✓ | | | |
| Leave requested | | | | ✓ (every admin) |
| Leave approved / rejected | | ✓ | | |

- A walk-in patient without a login gets nothing (no account to show it in), as today without a phone.
- Moving a visit gets a message in the app only; its SMS / email stays a follow-up.
- Front-desk and admin messages go to every active account of that role.

**Where it lives:** a new module **`inbox`**: the table `inbox_messages` (Flyway: user, kind, title, text, link, created, read), `Inbox` (public API: to a user, to every user of a role), the endpoints, and listeners for the other modules' events. It depends on common, identity, staff, appointments and billing; none of them knows about it (they publish events, as billing learns about visits today). The messages are written inside the transaction that changes the visit or the bill, so a change that rolls back leaves none.

**Events** (published inside the change's transaction):
- appointments: the existing `AppointmentNotificationEvent` also carries the visit's id and the patient's and doctor's user ids; a new kind `MOVED` (no SMS / email).
- billing (new public API): `BillNotificationEvent` — a bill made, paid, due for refund, refunded — with the bill, the patient's user id, the amount and the method.
- staff: `LeaveRequestedEvent`, and `LeaveDecidedEvent` (approved or rejected) next to the existing `DoctorLeaveApprovedEvent`.

**Endpoints** (any logged-in user, always their own): `GET /api/inbox` (the newest 50 of the last 90 days and the number unread), `GET /api/inbox/unread-count` (cheap, for the 30-second poll), `POST /api/inbox/{id}/read`, `POST /api/inbox/read-all`. Another user's message is 404. A nightly job deletes messages older than 90 days.

**Frontend:** a bell in the staff portals' top bar and in the patients' header, with the number unread (none shown at 0; "9+" above 9). It opens a panel: the newest first, unread ones marked (not by colour alone), the time ("5 min ago", "Yesterday, 10:05"), **Mark all as read**; a message opens its page (My appointments, My account → Bills, a bill, Leave, Leave approvals) and is marked read. The count is asked for every 30 seconds while logged in, and at once when the tab is shown again; the panel loads when it opens.

## Steps

| Step | Work | Checked by |
|---|---|---|
| NC.1 | **The inbox (backend):** the `inbox` module, `inbox_messages` (Flyway), `Inbox`, the endpoints, the clean-up, the module rules. | End-to-end tests (own messages only, unread count, read, read all, the 50 / 90 days, access); unit tests; mutation checks |
| NC.2 | **Visits and leave (backend):** the appointments event with its recipients and `MOVED`, the leave events, the inbox's listeners and texts. | End-to-end tests (book, move, cancel, leave, reminder, walk-ins without a login, a family member's visit; leave requested, approved, rejected); mutation checks |
| NC.3 | **Bills and payments (backend):** `BillNotificationEvent`, the listener and texts. | End-to-end tests (check-in, payment at the desk and online, refund due to the patient and the desk, refund given); mutation checks |
| NC.4 | **The bell (frontend):** the top bar and the header, the panel, the 30-second poll. | Tests; mutation checks; screenshots |
| NC.5 | **Docs** (README, module READMEs, the end-to-end test guide, the ideas doc, the MySQL runbook, the module plan), final check, push and PR link. | Full backend and frontend runs; build |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| NC.0 Plan + decisions | ✅ done | 2026-10-19 | This file. |
| NC.1 The inbox (backend) | ✅ done | 2026-10-19 | **New module `inbox`** (`hospital-inbox`; registered in the parent and app POMs, `tools/check_module_deps.py`, `MULTI_MODULE_PLAN.md`; the module diagrams). **Database:** `inbox_messages` (Flyway `inbox/V2026_10_19_2__inbox_messages.sql`: user — deleted with the account —, kind, title, text, link, created, read; indexed by user and time). **`Inbox`** (public API): `toUser` (nothing for a null user) and `toRole` (every active account of the role; new `UserService.activeUserIds` in identity), written in the caller's transaction; titles over 120 and texts over 500 characters cut with "…", links over 200 dropped. `InboxKind`: the twelve kinds of the plan. **`InboxStore`:** the newest 50 of the last 90 days and the unread count, marking one (only one's own; another user's is 404) or all read, deleting older ones (`InboxCleanup`, nightly at 04:15). **Endpoints** (`InboxController`, `InboxSecurityRules`: any logged-in user): `GET /api/inbox`, `GET /api/inbox/unread-count`, `POST /api/inbox/{id}/read`, `POST /api/inbox/read-all`. **Tests:** new `InboxH2Test` (3): own messages only, newest first, read, read all, anonymous refused; the newest 50, cut texts, a dropped link, 90 days, the nightly clean-up; a role message to every active account, not a deactivated one. **Mutation checks:** 12, all killed by a named test. Endpoint and access snapshots: +4 lines each. The MySQL migration test's list. The inbox README. Checker OK; full backend run green (223 tests, 1 skipped). |
| NC.2 Visits and leave (backend) | ✅ done | 2026-10-19 | **appointments:** `AppointmentNotificationEvent` carries `Who` (the appointment, the patient's login — the account holder's —, the doctor's) and a new kind `MOVED`, published when a visit gets another slot (no SMS / email: `AppointmentNotificationListener` skips it); every publisher builds it with `AppointmentService.notificationEvent` (also administration's). **staff:** `LeaveRequestedEvent` (the doctor's name, or the user name) and `LeaveDecidedEvent` (approved or rejected); applying for leave is now one transaction. **inbox:** `VisitMessages` (booked, moved, cancelled to the patient and the doctor; cancelled by the doctor's leave and the reminder to the patient; links to My appointments, the doctor's appointments, booking), `LeaveMessages` (a request to every admin with the days and the reason, the decision to the one who asked), `Texts` ("Tue 21 Oct", "10:00–10:30", "15–17 Oct"). An event without recipients (the emails' own tests) is skipped. Walk-ins without a login: only the doctor is told. **Tests:** new `InboxVisitsH2Test` (2): booked, moved, the reminder, cancelled; a family member's visit; walk-ins; a leave request, its approval (the doctor and the cancelled patient) and a rejection. `NotificationsH2Test` builds its events without recipients; `LeaveRequestServiceTest` checks the new events (a rejection is told now). **Mutation checks:** 13, all killed by a named test. Appointments, staff and inbox READMEs; module diagrams (inbox uses notifications, staff and appointments). **Found on the way:** the first full run failed in `LeaveRequestServiceTest`: the events took the leave id as a number, and its leaves have none yet; they take `Long` now. Checker OK; full backend run green (225 tests, 1 skipped). |
| NC.3 Bills and payments (backend) | ✅ done | 2026-10-19 | **billing** (its first public API): `BillNotificationEvent` (`BILL_READY`, `PAID`, `REFUND_DUE`, `REFUNDED`) with the bill, its patient account's user id, names, visit date, total and method, published by `BillNotifications` inside the change's transaction: a bill made or reopened at check-in, paid at the desk or online, due for refund when a paid visit is cancelled, refunded at the desk or online. **inbox:** `BillMessages` — to the patient "Your bill is ready", "Payment received", "Refund due", "Refund given" (My account), and a refund due to every receptionist with the bill's page; `Texts.rupees` writes "₹1,25,000.00" with Indian grouping (Java's currency format can't), `Texts.method`. **Tests:** new `InboxBillsH2Test` (made, paid by UPI, due for refund — the patient and the desk, not the admins —, refunded in cash; a walk-in's bill reaches nobody until a refund is due); `OnlinePaymentH2Test` +2 checks (an online payment told once although confirmed twice, an online refund); new `TextsTest` (2). **Mutation checks:** 12, all killed by a named test. Billing and inbox READMEs; module diagrams. Checker OK; full backend run green (228 tests, 1 skipped). |
