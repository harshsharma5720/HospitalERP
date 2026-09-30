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
