# Walk-in registration at the front desk — plan

**Branch:** `feature/walk-in-registration` (from `main` = 4c68cd9)
**Why:** a patient who walks in can't be booked unless they first sign up in the app themselves (email, password, OTP). The backend can book for an existing patient, but there is no front-desk booking screen at all — receptionists can only look at appointments.
**Idea list:** [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md#not-implemented-yet) — "not implemented yet" #10.

## Decisions (2026-10-08)

| Question | Decision |
|---|---|
| What is created | A **patient record without a login**: name, phone, gender, age (email and date of birth optional). Booked by the front desk; visits and medical records are kept like everyone else's; counted on the admin dashboard. No generated passwords. They can still sign up in the app on their own later. |
| Same phone number | **Show the matches and let the receptionist choose** — book for one of them, or create a new record anyway (families often share a number). |
| Booking | **One form:** find by phone or enter a new patient, pick a doctor; the earliest free slot from now on (today, otherwise the next days) is preselected and can be changed; one click registers and books. |

## Design

- **patients:** `patient.email` becomes optional and `patient.created_at` is added (Flyway); every new profile gets a creation time. `PtInfoService` gets `registerWalkIn(...)` and `findByPhone(phone)` (matches on the last 10 digits, so `+91 98765 43210` and `9876543210` are the same number). Both are recorded in the audit log (new actions: walk-in registered, patients searched by phone).
- **appointments** (front-desk endpoints, under `/api/receptionist/` — receptionists and admins): search patients by phone; the next free slots of a doctor; and **register-and-book in one transaction** — if the slot is gone, no patient record is left behind either. The usual booking rules, notifications (SMS to the phone given) and audit entries apply.
- **Admin dashboard:** active patients and new patients include the walk-in records (records without a login).
- **Frontend:** a **Walk-in** page for receptionists (and admins): phone → matches or a new-patient form → doctor → slot → Book → confirmation.

## Steps

| Step | Work | Checked by |
|---|---|---|
| W.1 | **Patient records without a login:** migration, entity, `registerWalkIn`, `findByPhone`, audit actions, dashboard counts. | Query / service tests; MySQL migration test; dashboard test |
| W.2 | **Front-desk API:** search by phone, next free slots, register-and-book (new or existing patient) in one transaction. | End-to-end tests (new patient, shared phone, slot already taken → nothing left behind, notifications, audit); endpoint and access snapshots updated on purpose |
| W.3 | **Frontend:** the Walk-in page and a navigation link; audit log labels for the new actions. | Frontend tests + build |
| W.4 | **Docs:** module READMEs, README, MySQL runbook (migration), idea list status. | — |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| W.0 Plan + decisions | ✅ done | 2026-10-08 | This file. |
| W.1 Patient records without a login | ✅ done | 2026-10-08 | Flyway `patients/V2026_10_08_1__patients_walk_in.sql`: `patient.email` optional (the unique key stays — several records without email are fine), `patient.created_at` added. `PtInfo`: email optional, `createdAt` (set for new walk-ins from the `Clock` and for new accounts from the account's creation time). `PtInfoService.registerWalkIn(WalkInPatient)`: name (≤ 100), phone (spaces / dashes / brackets removed, then `+` and 10–14 digits), gender required; date of birth (not in the future) and email (checked, blank = none) optional; audit `WALK_IN_REGISTERED`. `findByPhone`: at least 10 digits; a cheap first filter on the last 4 digits, then the last 10 digits compared exactly, so "+91 90000 11111", "(900) 001-1111" and "9000011111" are the same number; audit `PATIENTS_SEARCHED` ("by phone ...1111, 3 found" — not the full number). `PtInfoDTO.hasLogin`. Dashboard: active patients = active patient accounts + walk-in records; new patients per day = new accounts + new walk-ins; "counted since" = the earlier start. New `WalkInPatientsH2Test` (shares FeatureFlowH2Test's context): a record without login / email, stored phone, creation time, audit; refused details (name, phone length, a letter O, gender, future birth date, email); search across number formats and a family (two walk-ins + an app account with the same number; `hasLogin`), short numbers refused, audit details. `DashboardServiceTest` covers the walk-in counts. `DatabaseMigrationMySqlTest`: 8 migrations, `email` nullable, existing patients keep their email and get no creation time. MySQL runbook, README migrations table, patients and administration READMEs. Checker OK, 136 backend tests (1 skipped). |
