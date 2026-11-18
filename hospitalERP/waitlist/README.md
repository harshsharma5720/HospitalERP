# waitlist (`hospital-waitlist`)

The waitlist ([docs/WAITLIST_PLAN.md](../../docs/WAITLIST_PLAN.md)): a patient — or one of their family members — waits for a doctor on a day that has no free time left, in one shift or either, and is told when a time that day frees up. Joined by the patient in the app or by the front desk for anyone, also a walk-in record without a login.

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
- `WaitlistEntry` / `WaitlistRepository`, `WaitlistStatus`, `JoinRequest`, `WaitlistEntryDTO`.

## Database

Flyway `waitlist/V2026_11_18_1__waitlist_entries.sql`: `waitlist_entries`. Each entry holds the patient, the family member, the doctor, the day, the shift, the status, who joined it and when, when they were told and when it ended. An entry is deleted with its patient, family member or doctor (`ON DELETE CASCADE`). Indexed by doctor and day, by patient, and by day.

## Tests

End-to-end in `app`, `WaitlistH2Test`:
- **Joining:** a full shift only; for a family member; joining again; either shift; days, doctors and family members that can't be waited for; already booked; the limit of 5.
- **Lists and access:** the patient's own list, without past days; leaving; the desk's day list for any record, also a walk-in; removing; patients only on `/api/waitlist`.
- **Endings:** a booking and a move end a wait; a deactivated patient and a deactivated doctor; a removed family member.

Unit, in `scheduling`: `SlotServiceTimesAheadTest` (whether a shift has a time that can still free up).
