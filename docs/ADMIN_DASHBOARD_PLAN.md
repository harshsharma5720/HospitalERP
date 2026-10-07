# Real admin dashboard — plan

**Branch:** `feature/admin-dashboard` (from `main` = 0b1fa98)
**Why:** the admin dashboard (`/admin/dashboard`) shows invented numbers — "Sales Value $10,567", "Customers 345k", "Revenue", "Traffic Share", "Page Visits" — none of which come from the system (there is no billing and no visitor tracking). An admin can't tell how the hospital is doing.
**Idea list:** [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md#not-implemented-yet) — "not implemented yet" #7.
**Status:** ✅ done (2026-10-07) — see [Result](#result).

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
| B.3 Dashboard endpoint | ✅ done | 2026-10-06 | `GET /api/admin/dashboard?days=` (`administration.web.DashboardController`, admin-only area; default 7, only 7 / 30 / 90, else 400). `DashboardService` (today from the `Clock`) returns `DashboardDTO`: `period` (days, from, to), `today` (all appointments, completed, upcoming, cancelled; doctors on leave; active patients / doctors / receptionists), `trend` (one entry per day: completed, upcoming, missed, cancelled, new patients), `cancellations` (appointments, cancelled by patient / by doctor, cancellation rate, missed, missed rate — % with one decimal, 0 without a denominator), `newPatients` (count, `countedSince`), busiest specializations and doctors (top 5). New `DashboardServiceTest` (administration's first unit test; fixed date and figures: upcoming / missed split, today, sums, both rates worked out by hand, empty period, only 7 / 30 / 90). Test-first `FeatureFlowH2Test.adminDashboardShowsRealFigures` failed before (no endpoint) and passes now; because other tests share the database, it checks how the figures **change** after it creates a doctor, a patient and four appointments (completed and never-completed yesterday, upcoming and cancelled today — moved there with SQL, since bookings are only possible for the future), plus 30 / 90 days, a bad period (400) and a patient (403). Snapshots updated on purpose: endpoint +1 line, access +1 line (admin only). Administration README. Checker OK, 121 backend tests (1 skipped). |
| B.4 Frontend | ✅ done | 2026-10-07 | `AdminDashboard.js` rewritten — the fake Sales / Customers / Revenue / Traffic / Page-visits cards are gone. Period buttons 7 / 30 / 90 days (`aria-pressed`; a late answer to an earlier click is ignored, old figures stay while the new ones load). **Today** tiles (appointments with completed / upcoming / cancelled, doctors on leave, active patients / doctors / receptionists); **period** tiles (appointments, cancelled %, missed %, new patients "counted since …"); **appointments per day** as a stacked column chart (`AppointmentTrendChart.js`, Recharts): completed at the base, then missed / upcoming, cancelled on top; ≤ 24 px columns, 2 px gaps, rounded top segment only, solid hairline grid, legend in ink with swatches, tooltip per day, plus a **table view** ("Show the numbers"). New patients are a tile, not part of the appointment chart (a different measure). **Busiest** specializations and doctors as ranked lists with one-hue bars. Colors (`AdminDashboard.css`, CSS variables, light and `.dark` steps): the dataviz reference palette, chosen so that orange and yellow (the one pair that fails when touching) are missed / upcoming, which never share a column; the touching pairs passed the palette validator on the white card and on the dark card `#1e293b` (light: aqua and yellow below 3:1 contrast → the table view is the required relief). Rendered with sample data and screenshotted in light and dark mode (headless Edge) to check the layout; fixed the selected period button, which was invisible in dark mode. `admin/api.js` `getDashboard`; `dashboardFormat.js` (time-zone-free dates, Indian number grouping, specialization names). Tests: `AdminDashboard.test.js` (6: real figures and no placeholder text, table view, period buttons, stale answer ignored, empty period, error) and `AppointmentTrendChart.test.js` (real Recharts render: stacking, color classes, rounded tops only, empty segments not drawn). README feature table. 35 frontend tests green; `CI=true npm run build` compiles with 0 warnings. |
| B.5 Docs | ✅ done | 2026-10-07 | `CODE_REVIEW_AND_IDEAS.md` section 11: #7 done, the "partly done" admin-dashboard entry removed, suggested order (5 and 4 next; 8 can reuse the per-doctor queries) and a summary of this feature; the section 7 link follows the renamed heading. The module READMEs (appointments, identity, staff, administration), README (feature, admin portal and migrations tables) and the MySQL runbook were already updated in B.1–B.4. One deviation from the decisions, on purpose: new patients are a tile and a table column, not a line in the appointment chart — mixing two different measures in one chart misleads. |

## Result

The admin dashboard shows **real figures** instead of invented ones: today's appointments, doctors on leave and active accounts; for the last 7 / 30 / 90 days the appointments per day (completed, upcoming, missed, cancelled), cancellation and missed rates, new patients and the busiest specializations and doctors. Each module counts its own data with grouped queries; administration combines them in `GET /api/admin/dashboard`. Backend 121 tests passing (1 skipped without a MySQL `DB_URL`), frontend 35, module checker OK.

**Before running it on the real database:** the branch adds one Flyway migration (`users.created_at`; existing accounts stay empty). On a database that is already upgraded it runs by itself on the next start; otherwise follow [MYSQL_FLYWAY_UPGRADE.md](MYSQL_FLYWAY_UPGRADE.md).

**Possible follow-ups:** bookings for the coming days; doctor utilisation against their weekly schedule; revenue once billing (#18) exists; doctor analytics (#8) reusing the per-doctor queries.
