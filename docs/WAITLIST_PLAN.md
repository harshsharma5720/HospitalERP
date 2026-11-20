# Waitlist — plan

When a doctor's day is full, a patient today can only keep looking. Times do free up — a patient cancels, moves a visit, or an account is closed — but nobody waiting for them knows. This adds a **waitlist**: a patient (or the front desk for them) waits for a doctor on a day, and everyone waiting is told as soon as a time that day frees up. Idea #13 in [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md); it builds on the notification centre ([NOTIFICATION_CENTRE_PLAN.md](NOTIFICATION_CENTRE_PLAN.md)).

## Decisions (2026-11-18)

| Question | Decision |
|---|---|
| What a patient waits for | **A doctor on one day**: either shift, or only the morning or only the evening. Joined from a full day on the booking page. |
| When a time frees up | **Everyone waiting is told at once; whoever books first gets it.** No slot is held or booked for anyone. |
| How they are told | **The bell** (the notification centre) and **SMS**. No email. |
| Who adds someone | **Patients** in the app, for themselves or a family member, and **the front desk**, for anyone — a walk-in record without a login is told by SMS only. |
| Way of working | Each finished, tested step is committed without asking; the branch is pushed at the end with the PR link. |

## Rules (defaults; easy to change)

- **An entry** is a person (a patient, or one of their family members), a doctor, a day and a shift (`MORNING`, `EVENING`, or either).
- **Joining** is refused when:
  - the day is in the past or outside the booking window (30 days);
  - the doctor is deactivated, on leave that day, or doesn't work then;
  - the doctor still has a free time then ("There are free times then — book one");
  - the person already has a booking with that doctor that day.
- **Joining twice** for the same person, doctor and day changes the shift instead of adding a second entry.
- **A limit:** at most 5 waiting entries per patient account, family members included.
- **Telling:** a time that frees up in the shift waited for, still in the future, with an active doctor who isn't on leave.
  - Each waiting entry is told by the bell (to the account holder, for a family member too) and by SMS (to the patient record's phone).
  - The same entry is told **at most once every 30 minutes**, so several times freed together (e.g. a closed account's visits) send one message.
  - The SMS stops being sent once the time has started.
- **An entry ends when:**
  - the person books with that doctor that day (in the app, at the desk or as a walk-in);
  - they leave it, or the desk removes it;
  - the doctor's leave covering that day is approved, or the doctor is deactivated (they are told);
  - the family member is removed, or the patient's account is deactivated (not told).
- **Once the day has passed,** an entry is no longer shown. Entries are deleted 30 days after their day.
- **Not audited:** a waitlist entry isn't part of the medical record (like a booking).

## Design

**A new module `waitlist`** (`hospital-waitlist`). It depends on common, identity, notifications, patients, staff, scheduling, appointments and inbox; nothing depends on it.
- **Table:** `waitlist_entries` (Flyway `waitlist/`): patient, family member (optional), doctor, day, shift (empty = either), status (`WAITING`, `BOOKED`, `ENDED`; leaving deletes it), when it was joined and by whom, and when it was last told.
- **Rules in one place:** `WaitlistService`.
- **What it listens to:**
  - **New: `SlotFreedEvent`** (scheduling), published by `SlotService.releaseSlot` when a time becomes bookable again. It covers every cancellation, a move's old time, and a closed account's visits.
  - The appointments module's booking event (the entry ends as booked).
  - `DoctorLeaveApprovedEvent`, the deactivation of a doctor or patient, and `RelativeDeletedEvent`.
- **How it tells:** the bell through `Inbox.toUser` (new kinds `WAITLIST_TIME_FREE` and `WAITLIST_ENDED`); SMS through `NotificationOutbox.queueSms`.
  - Both are written in the same transaction as the cancellation, so a change that rolls back tells nobody.
- **Endpoints:**
  - patients: `POST /api/waitlist`, `GET /api/waitlist/my`, `DELETE /api/waitlist/{id}`;
  - front desk and admins: `GET /api/receptionist/waitlist?date=&doctorId=`, `POST /api/receptionist/waitlist`, `DELETE /api/receptionist/waitlist/{id}`.

**The app:**
- **Booking page:** on a full day, "Join the waitlist" (for whom, the shift).
  - Opens with a doctor, day and shift chosen from a link (`/appointments?doctorId=3&date=…&shift=…`), so the bell's message leads straight to the free time.
- **My appointments:** the patient's waitlists, with **Leave**.
- **Front desk:**
  - a **Waitlist** page: a day (today by default) and a doctor; who is waiting in the order they joined, with phone, shift and whether and when they were told; **Book** (walk-in booking with the patient and doctor chosen) and **Remove**;
  - on a patient's page, **Add to a waitlist** for the patient or a family member.

## Steps

| Step | Work | Checked by |
|---|---|---|
| WL.1 | **Joining and leaving (backend):** the module, table and rules, the patients' and the desk's endpoints, entries ending on a booking, a removed family member or a closed account. | Unit tests; end-to-end tests (each rule, the limit, both roles, access); snapshots; module checks; mutation checks |
| WL.2 | **Telling them (backend):** `SlotFreedEvent`, the bell and SMS, at most once per 30 minutes, the doctor's leave or deactivation ending entries, the 30-day clean-up. | End-to-end tests (a cancellation, a move, a closed account, the shift, the 30 minutes, a walk-in by SMS only, leave); mutation checks |
| WL.3 | **The app (frontend):** the booking page (join; opened from a link), My appointments' waitlists, the desk's Waitlist page and the patient page's Add. | Tests; mutation checks; screenshots |
| WL.4 | **Docs** (README, module READMEs, the end-to-end test guide, the ideas doc, the MySQL runbook), final check, push and PR link. | Full backend and frontend runs; build |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| WL.0 Plan + decisions | ✅ done | 2026-11-18 | This file. |
| WL.1 Joining and leaving | ✅ done | 2026-11-18 | New module `waitlist` (Flyway `waitlist/V2026_11_18_1__waitlist_entries.sql`; an entry goes with its patient, family member or doctor). `WaitlistService`: the rules above; joins run without one outer transaction, because `SlotService` makes a day's slots in its own. Endpoints `/api/waitlist` (patients) and `/api/receptionist/waitlist` (desk, admins). Ends on a booking or a move with the doctor that day (`AppointmentNotificationEvent`) and on a deactivated account — new `UserDeactivatedEvent` (identity), as deactivation published nothing. New module APIs: `SlotService.hasTimesAhead`, `AppointmentService.hasBookingWith`. `WaitlistH2Test` (3), `SlotServiceTimesAheadTest` (3); `DatabaseMigrationMySqlTest` has the new migration. Endpoint and access snapshots: the 6 new URLs. Mutation checks: 30 of 30 killed. The full run found that a notification event can come without an appointment (`NotificationsH2Test`: `who` null); the listener skips it now. Full backend: 249 tests (1 skipped), MySQL 4 of 4. |
| WL.2 Telling them | ✅ done | 2026-11-18 | `SlotService.releaseSlot` publishes `SlotFreedEvent` when a time is bookable again; the waitlist looks at it as the change commits (`BEFORE_COMMIT`, still in its transaction): if `SlotService.isOpenForBooking` (free, not started, the doctor active and not away), everyone waiting for that doctor, day and shift (or either) is told in the order they joined — the bell (`WAITLIST_TIME_FREE`, opening the booking page on that day) and an SMS (`HOSPITAL_NAME`; in the app, or `HOSPITAL_PHONE` / the front desk for a record without a login; not after the time starts) — at most once in 30 minutes per wait. An approved leave and a doctor's deactivation end the waits and tell them (`WAITLIST_ENDED`); a patient's deactivation ends them quietly. `WaitlistCleanup` deletes waits 30 days after their day (04:20). `WaitlistTellingH2Test` (4), `WaitlistMessagesTest` (4), `SlotServiceFreedTest` (2). Mutation checks: 32 of 32 killed — one survived at first (a time already started, e.g. a no-show cleared later, was told), and a test was added. |
| WL.3 The app (frontend) | ✅ done | 2026-11-18 | `features/waitlist`: the api, `JoinWaitlist` (on a full shift of the booking page, for the person chosen; When: morning, evening or any time; already waiting: shown with Leave), `MyWaitlist` (My appointments: each wait with Free times and Leave, a time that freed up said so), `WaitlistPage` (the desk's, `/waitlist` and `/admin/waitlist`: a day and a doctor kept in the address; who, phone, shift, joined, told, status; Book — Walk-in with the patient and doctor chosen, not for a family member — and Remove), `AddToWaitlist` (the patient page's dialog). The booking page opens on a linked doctor, day (up to 30 days ahead, added to the day buttons) and shift; Walk-in takes `?doctorId=`; the desk menu has **Waitlist**. Found on the way: Walk-in's new effect used `loadSlots` before it was defined (every Walk-in test failed — moved below it). Tests: 324 (all frontend). Mutation checks: 46 of 46 killed — one survived at first: `JoinWaitlist` reset its state in an effect that changing the day, doctor or shift never needed (the times reload and it remounts); it now gets a key per person, doctor, day and shift, so another person's reason doesn't stay. Screenshots: the booking page's waitlist box (light, dark, phone, already waiting), My appointments, the desk's Waitlist page (light, dark, phone), the add dialog. |
| WL.4 Docs and final check | ✅ done | 2026-11-18 | README (features, migrations, the new module), the waitlist / scheduling / identity / appointments / inbox READMEs, [END_TO_END_TEST_GUIDE.md](END_TO_END_TEST_GUIDE.md) feature 28, [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md) (#13 done, status, suggested order), [MYSQL_FLYWAY_UPGRADE.md](MYSQL_FLYWAY_UPGRADE.md) (the new migration and table; last tested 2026-11-18 with all 17). Final check: 259 backend tests (1 skipped) with the MySQL test, 324 frontend tests, the production build. Mutation checks over the idea: 108 of 108. |
