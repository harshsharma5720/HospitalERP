# Doctor analytics — plan

The doctor's dashboard shows three counts: total, completed and pending appointments. To get them, the browser downloads every appointment the doctor ever had. The hospital now records much more: when patients are checked in and called in (today's queue), which visits were missed or cancelled, and what each visit brought in (billing). This piece of work turns that into a doctor's analytics for the last 7, 30 or 90 days, for the doctor and for admins. It is idea #8 in [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md).

## Decisions (2026-10-16)

| Question | Decision |
|---|---|
| Figures | **Visits per day** (completed, missed, upcoming, cancelled) with the **no-show and cancellation rates**; **busy hours** (by hour of the day and by weekday); **waiting time** (check-in to being called in). New vs returning patients is left out. |
| Who sees them | **The doctor**, their own, on their dashboard; **admins**, any doctor's, from the Doctors page. |
| Earnings | **Yes, for both:** what the period's visits brought in (paid bills), refunds, still unpaid, and the average per paid visit. |
| Periods | 7, 30 or 90 days ending today, like the admin dashboard. |
| Way of working | Each finished, tested step is committed without asking; the branch is pushed at the end with the PR link. |

## Design

**Rules (the same as the admin dashboard, so the figures agree):**
- Dates are visit dates.
- "Completed" is status COMPLETED or the old completed flag. An open visit is "upcoming" from today on and "missed" before today.
- Cancellation rate = cancelled ÷ all visits. No-show rate = missed ÷ past visits that weren't cancelled.

**Visits (appointments module):**
- `DoctorAnalytics` (public API) builds `DoctorAnalyticsDTO`:
  - the period and the doctor;
  - totals and rates;
  - visits per day (every day, zeros included);
  - busy hours: visits by the hour their slot starts, and by weekday, cancelled ones not counted;
  - waiting: for visits with both a check-in and a call-in time, the average and the longest wait in minutes, also per day, and how many visits were measured. The queue only records these since 2026-10-12, so older periods measure fewer visits.
- All of it comes from grouped queries filtered by doctor.
- Endpoints:
  - `GET /api/doctor/analytics?days=7|30|90` gives the logged-in doctor's figures (staff's `/api/doctor/**` rules: doctors and admins; an admin without a doctor profile gets 404);
  - `GET /api/admin/doctors/{doctorId}/analytics?days=` gives any doctor's, for admins only.

**Earnings (billing module):**
- `DoctorEarnings` builds `DoctorEarningsDTO`, from the bills of the period's visits for that doctor:
  - **earned**: bills paid and not refunded, by visit date, also per day;
  - the number of paid visits and the average per paid visit;
  - **refunds**: refund due or refunded;
  - **unpaid**: bills still open.
- Endpoints: `GET /api/doctor/earnings?days=` and `GET /api/admin/doctors/{doctorId}/earnings?days=`.
- Billing knows bills; the appointments module stays unaware of money.

**Frontend:**
- `features/doctors/analytics/DoctorAnalytics.js` takes a doctor, or none for "me". It has:
  - the period switch;
  - stat tiles: visits, no-show rate, cancellation rate, average wait, earned;
  - "Visits per day" as stacked bars, reusing the admin dashboard's chart and colours, moved to `shared/dashboard/`;
  - "Busy hours" and "Busy days" as single-colour bars;
  - "Waiting time" as the average per day;
  - "Earnings per day".
  
  Each chart has a hover tooltip and a table view, and works in dark mode.
- The doctor's dashboard keeps its greeting and quick links, and shows `DoctorAnalytics` in place of the three old counts. The full appointment lists are no longer downloaded.
- Admins open **Analytics** for a doctor from the Doctors page: `/admin/doctor/:doctorId/analytics`.

## Steps

| Step | Work | Checked by |
|---|---|---|
| DA.1 | **Visit figures (backend):** queries, `DoctorAnalytics`, the two endpoints. | End-to-end tests with known visits (per day, rates, busy hours, waiting, other doctors not counted, access); mutation checks |
| DA.2 | **Earnings (backend).** | End-to-end tests (paid, refunded, unpaid, other doctors, access); mutation checks |
| DA.3 | **Doctor's dashboard (frontend):** the analytics, the charts moved to `shared/dashboard/`. | Tests; mutation checks; palette check; screenshots |
| DA.4 | **Admin:** a doctor's analytics from the Doctors page. | Tests; screenshots |
| DA.5 | **Docs**, final check, push and PR link. | Full backend and frontend runs; build |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| DA.0 Plan + decisions | ✅ done | 2026-10-16 | This file. |
| DA.1 Visit figures (backend) | ✅ done | 2026-10-16 | **Queries:** three grouped queries for one doctor in `AppointmentRepository`: rows per day by status and the completed flag; per day and slot start time, without cancelled visits; the check-in and call-in times of visits that have both. `AppointmentStatistics`' per-day counting is shared, so the doctor's days follow exactly the admin dashboard's rules (new `perDayForDoctor`). **`DoctorAnalytics`** (public API) gives `mine` (the logged-in doctor; 404 without a doctor profile) and `forDoctor` (admins). Only 7, 30 or 90 days, ending today (`Clock`). It returns `DoctorAnalyticsDTO`: the period and the doctor (the user name while the profile has none); totals with the cancellation rate (cancelled ÷ all) and the no-show rate (missed ÷ past visits not cancelled, today left out, as on the admin dashboard); visits per day (every day); by hour (every hour from the first to the last with visits) and by weekday (Monday first); waiting (measured, average to one decimal, longest, per day; null when nothing was measured). **Endpoints** (`DoctorAnalyticsController`): `GET /api/doctor/analytics?days=` (doctors and admins, through staff's rules) and `GET /api/admin/doctors/{id}/analytics?days=` (admins); 30 days by default. **Tests:** new `DoctorAnalyticsH2Test` (2, in FeatureFlowH2Test's context). One doctor has a cancelled, a missed, a completed (15-minute wait) and an upcoming (30-minute wait) visit on known days; another doctor's visits don't count. Checked: the totals, both rates, the 7 days, hours 10 to 16, the weekdays, the waits (average 22.5, longest 30, per day, empty days); the admin sees the same; 30 days, and 30 by default. A doctor without visits; 8 days refused; the admin path refused to the doctor; receptionists and patients refused; an admin without a doctor profile and an unknown doctor get 404. **Mutation checks** (each makes a named test fail): other doctors counted, today's open visits counted as missed, the no-show rate over all visits, cancelled visits in the busy hours, hours with gaps, the shortest wait as the longest, any period allowed. Endpoint and access snapshots: +2 lines each. Module docs: `DoctorAnalytics` is new public API. Appointments README. Checker OK; full backend run green (200 tests, 1 skipped). |
