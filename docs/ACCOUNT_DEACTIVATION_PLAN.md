# Account deactivation instead of deletion — plan

**Branch:** `feature/account-deactivation` (from `main` = 1602274)
**Why:** deleting a patient or doctor today also deletes their appointments, consultations and prescriptions. Medical records have to be kept (medical council record-keeping rules, DPDP Act 2023), and a mistaken delete can't be undone.
**Idea list:** [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md) — "not implemented yet" #1.

## Decisions (2026-09-30)

| Question | Decision |
|---|---|
| Delete buttons / endpoints | They **deactivate**. A separate **reactivate** exists. **Permanent delete** stays for admins, but only for accounts **without any appointments or medical records** (e.g. test accounts); otherwise it is refused with "deactivate instead". |
| Personal data on deactivation | **Kept** (name, email, phone). Reactivation is possible, records stay readable. Anonymisation can be a later, separate admin action. |
| Patients | May **deactivate their own account** (the existing `DELETE /api/patient/deleteAccount/{ptId}`); only an admin can reactivate. |

## What "deactivated" means

- The person **can't log in**, and tokens they already hold **stop working** on the next request.
- They **disappear** from the public doctor directory and from booking.
- Their **upcoming appointments are cancelled**: a doctor's patients are notified (as for an approved leave); a patient's booked slots are released.
- **All history stays**: appointments, consultations, prescriptions, leave requests.
- Admins still see the account (marked "deactivated") and can **reactivate** it.

## Steps

| Step | Work | Checked by |
|---|---|---|
| D.1 | **Database + entity:** `users.active` (not null, default true) and `users.deactivated_at` via a Flyway migration in `db/migration/identity/`; `User` gets both fields; `UserDTO` shows `active`. | MySQL migration test (upgrade + fresh install still identical), all tests green |
| D.2 | **Login and tokens:** a deactivated user gets a clear "account deactivated" error at login; the JWT filter rejects their existing tokens; password reset and OTP do nothing for them. | New tests (login refused, old token → 401) |
| D.3 | **Deactivate / reactivate use case** (administration): cancels upcoming appointments per role (notifications for a doctor's patients, slot release for a patient), stops a doctor's unused future slots; keeps all history. New `PUT /api/admin/users/{userId}/deactivate` and `/reactivate`. The existing delete endpoints deactivate; permanent delete is guarded (refused when history exists). | End-to-end H2 tests; endpoint and access snapshots updated on purpose |
| D.4 | **Hide deactivated accounts** from the doctor directory (all, by specialization, public profile), booking and slot availability. | Tests |
| D.5 | **Frontend:** Manage Users / Manage Doctors show the status with Deactivate / Reactivate buttons; Delete explains when it's refused; the login page shows "account deactivated". | Frontend tests + build |
| D.6 | **Docs:** module READMEs, README, idea list status. | — |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| D.0 Plan + decisions | ✅ done | 2026-09-30 | This file. |
| D.1 Database + entity | ✅ done | 2026-09-30 | `identity/V2026_09_30_1__identity_account_status.sql`: `users.active bit(1) NOT NULL DEFAULT b'1'`, `users.deactivated_at datetime(6)` (the types Hibernate expects on MySQL). `User` and `UserDTO` got `active` / `deactivatedAt` (the admin user list returns them). No behaviour change yet. `DatabaseMigrationMySqlTest`: on an existing database the new migration runs after the two drops and existing users stay active; a fresh install is still identical. MySQL runbook and README updated (3 migrations on upgrade; the two new columns are an expected "missing" row in the 5.3 comparison). Checker OK, 88 backend tests green (1 skipped). |
| D.2 Login and tokens | ✅ done | 2026-10-01 | `CustomUserDetailsService` marks a deactivated user `disabled`. `SecurityConfig`: account status is checked **after** the password (`AccountStatusUserDetailsChecker` as post-check, no pre-check), so a wrong password always gets the generic 401 and reveals nothing. Right password → **403 "This account has been deactivated. Please contact the hospital."** (`GlobalExceptionHandler`); it doesn't count towards the 5-attempt lock (`AuthService`). `JWTAuthenticationFilter` ignores the token of a disabled user → 401 on the next request (the frontend then logs out). `PasswordResetService` treats a deactivated account like an unknown one (same answer, no code, no reset). Test-first: new `FeatureFlowH2Test.deactivatedAccountCannotLogInAndItsTokensStopWorking` failed on the old code (token still worked) and passes now; it also checks wrong password → generic 401, reset answer identical to an unknown account, and login works again after reactivation. New `PasswordResetServiceTest` case. Checker OK, 90 backend tests green (1 skipped). |
| D.3 Deactivate / reactivate / guarded delete | ✅ done | 2026-10-01 | `UserAccountService` (administration): `deactivate` — patient: bookings from today on cancelled as by the patient (slot freed, notified); doctor: bookings from today on `CANCELLED_BY_DOCTOR`, patients notified, slots freed; past appointments, consultations, prescriptions, leaves, schedule all kept; then `users.active=false`. `reactivate` undoes it. `deletePermanently` only without any appointment (409 "…must be kept. Deactivate it instead."), reusing the old deletion flow. Admins can't deactivate or delete themselves (400). **Refinement of the plan:** a doctor's slots are not deleted on deactivation — they are created on demand from the weekly schedule, so D.4's guard (no availability / booking for a deactivated doctor) is enough and reactivation needs no rebuild. New endpoints (admin only): `PUT /api/admin/users/{userId}/deactivate`, `PUT …/reactivate` (both return the user with `active`), `DELETE /api/admin/users/{userId}` (permanent, guarded). The old delete URLs (`DELETE /api/admin/{id}`, `/api/patient/deleteAccount/{ptId}`, `/api/doctor/delete/{id}`, `/api/receptionist/delete/{id}`) now deactivate. appointments got `cancelUpcomingForPatient/ForDoctor`, `hasAnyAppointmentForPatient/ForDoctor`; identity `UserService.deactivate/reactivate`. Test-first: `FeatureFlowH2Test.deletingAccountsRemovesProfilesAndLogins` rewritten as `deactivatingAccountsKeepsHistoryAndCancelsUpcomingBookings` (self-deactivation, admin deactivates doctor and receptionist, history and consultation kept, statuses `CANCELLED_BY_PATIENT` / `CANCELLED_BY_DOCTOR`, freed slot bookable, only admins reactivate) and new `permanentDeleteOnlyForAccountsWithoutHistory` (delete without history works, refused with history, explicit deactivate, user list shows `active=false` + `deactivatedAt`, no self-lockout) — both failed on the old code. Endpoint snapshot +3 lines, access snapshot +3 lines (admin only), nothing else changed. Checker OK, 91 backend tests green (1 skipped). |
