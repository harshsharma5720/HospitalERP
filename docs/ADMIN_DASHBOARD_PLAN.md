# Real admin dashboard — plan

**Branch:** `feature/admin-dashboard` (from `main` = 0b1fa98)
**Why:** the admin dashboard (`/admin/dashboard`) shows invented numbers — "Sales Value $10,567", "Customers 345k", "Revenue", "Traffic Share", "Page Visits" — none of which come from the system (there is no billing and no visitor tracking). An admin can't tell how the hospital is doing.
**Idea list:** [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md#not-implemented-yet) — "not implemented yet" #7.

## Decisions (2026-10-06)

| Question | Decision |
|---|---|
| Sections | **Today at a glance** (today's appointments by status, doctors on leave today, active patients / doctors / receptionists), **appointment trend** (per day: completed, upcoming, missed, cancelled), **cancellations & missed visits** (rate, by patient vs. by doctor; past appointments never completed), **busiest specializations & doctors**. All fake cards are removed. |
| Period | Buttons **7 / 30 / 90 days** (ending today); the chart and every period figure follow them. |
| New patients | **Add `users.created_at`** (Flyway migration). Existing accounts have no date, so "new patients" counts from the upgrade on, and the dashboard says since when. |

## Definitions

- **Period:** the last N days including today (N = 7, 30 or 90).
- An appointment counts on its **appointment date**. Status groups: **completed** (`COMPLETED`), **cancelled** (`CANCELLED_BY_PATIENT`, `CANCELLED_BY_DOCTOR`), and open (`SCHEDULED`, `CONFIRMED`): **upcoming** if its date is today or later, **missed** if the date has passed (never completed — most likely a no-show, or the doctor didn't record the visit).
- **Cancellation rate:** cancelled ÷ all appointments in the period. **Missed rate:** missed ÷ past appointments that weren't cancelled.
- **Busiest:** specializations and doctors with the most appointments in the period, cancelled ones not counted (top 5 each).
- **Active** accounts: not deactivated. Totals count login accounts (an old doctor row without a login isn't counted).

## Design

- Each module computes its own figures with count queries (no loading of whole tables): **appointments** (per day and status, per specialization, per doctor), **identity** (active accounts per role, new accounts per day), **staff** (doctors on leave on a day).
- **administration** combines them: `GET /api/admin/dashboard?days=7|30|90` (inside the admin-only `/api/admin/**` area; any other `days` is a 400).
- The frontend page is rewritten with the existing chart library (Recharts).

## Steps

| Step | Work | Checked by |
|---|---|---|
| B.1 | **Account creation time:** `users.created_at` (Flyway), set when an account is created; existing accounts stay empty. | MySQL migration test; end-to-end test; all tests green |
| B.2 | **Statistics queries** in appointments, identity and staff (count queries, by period). | Query tests with known data |
| B.3 | **Dashboard endpoint** in administration: `GET /api/admin/dashboard?days=` combining them. | End-to-end test with known data; endpoint and access snapshots updated on purpose |
| B.4 | **Frontend:** the real dashboard (period buttons, today, chart, cancellations & missed, busiest), fake cards removed. | Frontend tests + build |
| B.5 | **Docs:** module READMEs, README, idea list status, MySQL runbook. | — |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| B.0 Plan + decisions | ✅ done | 2026-10-06 | This file. |
| B.1 Account creation time | ✅ done | 2026-10-06 | Flyway `identity/V2026_10_06_1__identity_user_created_at.sql`: `users.created_at datetime(6)`, nullable — existing accounts stay empty. `User.createdAt` (`updatable = false`), set in `AuthService.createUser` from the `Clock` — the one place every account is created (self sign-up, admin's create use cases, first-admin bootstrap). Test-first `FeatureFlowH2Test.newAccountsRecordWhenTheyWereCreated` (a self-registered patient and an admin-created doctor both get a time between "before" and "now") failed on the old code (no column) and passes now. `DatabaseMigrationMySqlTest`: 6 migrations, existing users keep `created_at` NULL, upgrade and fresh install still identical. MySQL runbook (migration lists, expected log lines, counts with / without this branch), README migrations table and identity README updated. Checker OK, 111 backend tests (1 skipped). |
| B.2 Statistics queries | ✅ done | 2026-10-06 | **appointments:** public `AppointmentStatistics` — `perDay(from, to)` (one grouped query by date, status and the old `is_completed` flag, turned into `DailyAppointmentCounts`: completed / open / cancelled by patient / by doctor, every day of the range, zeros included), `busiestSpecializations` and `busiestDoctors` (grouped queries with `SpecializationCount` / `DoctorAppointmentCount`, cancelled ones left out, top N, ties by name). "Completed" = status `COMPLETED` or the old flag, the module's existing rule. **identity:** `UserService.countActiveAccountsPerRole` (every role, zeros included), `countNewAccountsPerDay(role, from, to)` (creation times of the period, grouped by day), `firstAccountCreationTime`. **staff:** `LeaveRequestService.countDoctorsOnLeave(date)` (approved, covering the day, active doctors, each once). New `DashboardStatisticsTest` (`@DataJpaTest`, 4 tests with known data: outcomes per day incl. an old row and an empty day, busiest lists without cancelled ones and the limit, active accounts and new accounts per day incl. a deactivated one, doctors on leave: overlapping leaves, pending, later, receptionist, deactivated doctor). Mutation check: without the old-flag rule the per-day test fails. `DatabaseMigrationMySqlTest.dashboardQueriesRunOnMySql` runs every new query on MySQL 8 (stricter GROUP BY). Module READMEs (appointments, identity, staff). No endpoint yet. Checker OK, 116 backend tests (1 skipped). |
