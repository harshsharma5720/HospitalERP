# waitlist (`hospital-waitlist`)

The waitlist ([docs/WAITLIST_PLAN.md](../../docs/WAITLIST_PLAN.md)): a patient — or one of their family members — waits for a doctor on a day that has no free time left, in one shift or either, and is told — the bell and SMS — when a time that day frees up. Joined by the patient in the app or by the front desk for anyone, also a walk-in record without a login (told by SMS only).

**Depends on:** common, notifications, identity, patients, staff, scheduling, appointments, inbox · **Used by:** app

## Public API — `com.itmonteur.hospitalerp.waitlist`

None: nothing calls it. It listens to the other modules' events.

## Endpoints — `waitlist.web`

| Controller | URLs |
|---|---|
| `WaitlistController` | `/api/waitlist`, patients only (`WaitlistSecurityRules`): `POST` `{doctorId, date, shift, relativeId?}` (`shift`: `MORNING`, `EVENING` or null for either), `GET /my` (their own waits and their family members', from today on, soonest first), `DELETE /{id}` (leave; another account's → 404). |
| `WaitlistDeskController` | `/api/receptionist/waitlist`, receptionists and admins (staff's rules): `GET ?date=&doctorId=` (a day — today when not given — every wait, by doctor and in the order they were joined, also the ended ones), `POST` `{patientId, relativeId?, doctorId, date, shift}`, `DELETE /{id}`. |

A wait (`WaitlistEntryDTO`): `id`, `patientId`, `relativeId`, `name` (the person waiting), `patientName` (the patient record), `phone`, `hasLogin`, `doctorId`, `doctorName`, `date`, `shift`, `status` (`WAITING`, `BOOKED`, `ENDED`), `joinedAt`, `joinedBy` (username), `toldAt`.

**Joining is refused when:**
- the day is in the past or more than 30 days ahead (400);
- the doctor is deactivated, away that day, or has no time ahead in that shift (400);
- the doctor still has a free time then: "There are free times then. Please book one." (409);
- the person already has a booking with the doctor that day (409);
- the account already has 5 waits (409).

Joining again for the same person, doctor and day changes the shift. Leaving or removing an ended wait → 409.

## Internal — `waitlist.internal`

- `WaitlistService`: the rules.
  - Joins run without one outer transaction, because `SlotService` makes a day's slots in its own.
  - `closeBooked` and `endForAccount` are used by the listeners.
- `WaitlistEndings`: listens in the change's transaction.
  - `AppointmentNotificationEvent` (booked or moved): the person's wait for that doctor and day ends as `BOOKED`.
  - `UserDeactivatedEvent` (identity): a deactivated patient's waits, and everyone's waits for a deactivated doctor, end as `ENDED`.
- `WaitlistTelling`: the telling.
  - `SlotFreedEvent` (scheduling), looked at as the freeing change commits (`BEFORE_COMMIT`): still in its transaction, so the messages commit with it, but when the change is complete — a doctor being deactivated is inactive by then.
  - If the time can still be booked (`SlotService.isOpenForBooking`), everyone waiting for that doctor, day and shift (or either) is told, in the order they joined — each wait at most once in 30 minutes (`toldAt`).
  - `DoctorLeaveApprovedEvent` (staff): the waits for those days end and are told.
- `WaitlistMessages`: the texts.
  - The bell to the account holder: `WAITLIST_TIME_FREE` ("A time is free: Dr. …", "Fri 20 Nov, 10:00–10:30, for Mira Verma. First come, first served: book it now.", opening `/appointments?doctorId=…&date=…&shift=…`) and `WAITLIST_ENDED` ("Waitlist ended: Dr. …", the reason: away on that day, or not seeing patients any more).
  - An SMS to the patient record's phone (`HOSPITAL_NAME` first): book it in the app — or, for a record without a login, call `HOSPITAL_PHONE` or contact the front desk. Not sent once the time has started, or after the day of an ended wait.
- `WaitlistCleanup`: every night at 04:20 (`app.waitlist.cleanup-cron`), deletes waits 30 days after their day.
- `WaitlistEntry` / `WaitlistRepository`, `WaitlistStatus`, `JoinRequest`, `WaitlistEntryDTO`.

## Database

Flyway `waitlist/V2026_11_18_1__waitlist_entries.sql`: `waitlist_entries`. Each entry holds the patient, the family member, the doctor, the day, the shift, the status, who joined it and when, when they were told and when it ended. An entry is deleted with its patient, family member or doctor (`ON DELETE CASCADE`). Indexed by doctor and day, by patient, and by day.

## Tests

End-to-end in `app`, `WaitlistH2Test`:
- **Joining:** a full shift only; for a family member; joining again; either shift; days, doctors and family members that can't be waited for; already booked; the limit of 5.
- **Lists and access:** the patient's own list, without past days; leaving; the desk's day list for any record, also a walk-in; removing; patients only on `/api/waitlist`.
- **Endings:** a booking and a move end a wait; a deactivated patient (not told) and a deactivated doctor; a removed family member.

`WaitlistTellingH2Test`: a cancelled time told to the morning's and either shift's waits, not the evening's — by the bell and SMS, a walk-in by SMS only; the quiet 30 minutes; a deactivated doctor's freed times not told, the waits ended and told; an approved leave over two days ends and tells those days' waits, not another doctor's; waits deleted 30 days after their day.

Unit: `WaitlistMessagesTest` (with `HOSPITAL_PHONE`: in the app or call; a record without a login; no phone, no SMS; when an SMS stops); in `scheduling`, `SlotServiceTimesAheadTest` (whether a shift has a time that can still free up) and `SlotServiceFreedTest`.
