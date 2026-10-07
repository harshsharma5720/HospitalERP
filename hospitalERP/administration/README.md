# administration (`hospital-administration`)

Admin use cases that span several modules: creating users of any role, the admin dashboard lists, leave decisions, and **account deactivation** (accounts are deactivated, not deleted — see [docs/ACCOUNT_DEACTIVATION_PLAN.md](../../docs/ACCOUNT_DEACTIVATION_PLAN.md)). It sits at the top of the module graph, so it may call every other module — and no module calls it.

**Depends on:** common, notifications, identity, audit, patients, staff, scheduling, appointments, clinical (uses all but notifications today) · **Used by:** nothing (only `app` assembles it)

## Public API — `com.itmonteur.hospitalerp.administration`

None. Nothing else may depend on administration.

## Events

- **Publishes:** nothing in normal use. Deactivating an account calls appointments (`cancelUpcomingForPatient` / `cancelUpcomingForDoctor`), which cancels the bookings and sends the notifications. (The permanent delete of a doctor still publishes `AppointmentNotificationEvent` for upcoming bookings, but it's only allowed when there are none.)
- **Listens to:** nothing.

## Endpoints — `administration.web`

| Controller | URLs |
|---|---|
| `AdminController` | `/api/admin/**` — create users, user and staff lists, leave approval/rejection, appointment overviews; account status: `PUT /users/{userId}/deactivate`, `PUT /users/{userId}/reactivate`, `DELETE /users/{userId}` (permanent, only without appointments — otherwise 409). The old `DELETE /{id}` deactivates. |
| `AccountController` | `DELETE /api/patient/deleteAccount/{ptId}` (the patient themself or an admin), `DELETE /api/doctor/delete/{id}` and `DELETE /api/receptionist/delete/{receptionistId}` (admin only) — despite the old names, these **deactivate**. |
| `AuditLogController` | `GET /api/admin/audit-log` — the [audit log](../audit/README.md), newest first. Optional filters `patientId`, `username`, `action`, `from` / `to` (days, `yyyy-MM-dd`, both included); `page` (from 0) and `size` (default 50, at most 100). Returns `{entries, page, size, totalEntries, totalPages}`; each entry has the patient's current name. A bad filter is a 400. |
| `DashboardController` | `GET /api/admin/dashboard?days=…` with 7, 30 or 90 (default 7; anything else is a 400) — the admin dashboard's figures for the last N days ending today: `period`, `today` (appointments by status, doctors on leave, active patients / doctors / receptionists), `trend` (per day: completed, upcoming, missed, cancelled, new patients), `cancellations` (counts and rates in %), `newPatients` (count, `countedSince`), `busiestSpecializations`, `busiestDoctors` (top 5). See [docs/ADMIN_DASHBOARD_PLAN.md](../../docs/ADMIN_DASHBOARD_PLAN.md). |

Access rules (`administration.web.AdministrationSecurityRules`, a `ModuleSecurityRules` bean): `/api/admin/**` admins only.

## Internal — `administration.internal`

| Class | Purpose |
|---|---|
| `AdminService` | The admin use cases, calling the other modules' services. |
| `DashboardService`, `DashboardDTO` | Combine the figures appointments (`AppointmentStatistics`), identity (`UserService`) and staff (`LeaveRequestService`) compute with their own count queries: open appointments become **upcoming** (today) or **missed** (before today); cancellation rate = cancelled ÷ all, missed rate = missed ÷ past appointments not cancelled. |
| `AuditLogQuery`, `AuditLogPage` | Reads the audit log for `AuditLogController` and adds the patients' names (`PtInfoService.findPatientNames`, one query per page), since the audit module can't depend on patients. |
| `UserAccountService` | Account lifecycle. **Deactivate**: cancels bookings from today on (patient: as if they cancelled, slot freed; doctor: `CANCELLED_BY_DOCTOR`, patients notified), then `users.active = false`; all history stays. **Reactivate** undoes it. **Delete permanently** only without any appointment: leave requests, then the role's data — consultations → appointments → (doctor: slots and weekly schedule) → profile, then the login account. Admins can't deactivate or delete themselves. Every change goes into the [audit log](../audit/README.md) — user created (`recordCreated`, called by `AdminService`), deactivated, reactivated, deleted, with the account's username and role in the details; a call that changes nothing records nothing. Uses only other modules' public services, never their repositories. |

## Configuration

None.

## Tests

`DashboardServiceTest` (how the figures are combined, rates, empty period, allowed periods). Deactivation, permanent delete, admin flows and their audit entries (`accountActionsAreAudited`) are covered by `FeatureFlowH2Test` and `SecurityRulesTest` (`app`).

Module diagram: [docs/modules/module-administration.puml](../../docs/modules/module-administration.puml).
