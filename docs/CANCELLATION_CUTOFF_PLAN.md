# Cancellation cut-off — plan

Patients can cancel or move an appointment in the app right up to its start, even 5 minutes before. The doctor's time is then lost: it's too late for the front desk to give the slot to someone else. This piece of work stops patients changing a visit themselves shortly before it starts. The front desk and admins can still change it. It is idea #9 in [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md).

## Decisions (2026-10-15)

| Question | Decision |
|---|---|
| Cut-off | **2 hours** before the visit starts, configurable as `APPOINTMENT_CHANGE_CUTOFF_HOURS`; 0 turns it off. |
| What it applies to | **Cancelling and moving to another time.** Changing only the reason or the patient's details stays possible. |
| Inside the cut-off | The visit card shows **"Less than 2 hours to go: to cancel or move it, please call +91 …"** with a tap-to-call link, instead of Cancel / Reschedule. Before the cut-off, the card says until when it can be changed. |
| Way of working | Each finished, tested step is committed without asking; the branch is pushed at the end with the PR link. |

## Design

- **The rule (appointments module, `AppointmentService`):**
  - A visit's **change deadline** is its start (the slot's date and start time) minus the cut-off.
  - A patient who cancels, or moves the visit to another slot, at or after that deadline gets **409** with a plain message that includes the hospital's phone number (`HOSPITAL_PHONE`) when it is set.
  - The front desk and admins are never limited. Doctors can't change appointments anyway.
  - "Now" comes from the application's `Clock`, as elsewhere.
- **For the app:** `AppointmentDTO.changeableUntil` is the deadline, set for upcoming visits (empty for cancelled or past ones, and when the cut-off is 0). The app needs nothing else: the cut-off itself is the visit's start minus `changeableUntil`.
- **Configuration:** `app.appointments.change-cutoff-hours=${APPOINTMENT_CHANGE_CUTOFF_HOURS:2}`, also in `docker-compose.yml`, `.env.docker.example` and the README.
- **Frontend (My appointments):** before the deadline, upcoming cards keep Cancel / Reschedule and add "You can cancel or move it until …". After it, the buttons are replaced by the call-the-hospital note (`HOSPITAL.phone` from `shared/hospitalInfo.js`). If the page was left open past the deadline, the server's message is shown.

## Steps

| Step | Work | Checked by |
|---|---|---|
| CC.1 | **The rule (backend):** deadline, 409 for patients inside it, `changeableUntil`, configuration. | End-to-end tests (inside / outside the cut-off, cancel and move, desk not limited, details still editable, cut-off 0); mutation checks |
| CC.2 | **My appointments:** until when, or call the hospital. | Tests; mutation checks; screenshots |
| CC.3 | **Docs and configuration files**, final check, push and PR link. | Full backend and frontend runs; build |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| CC.0 Plan + decisions | ✅ done | 2026-10-15 | This file. |
| CC.1 The rule (backend) | ✅ done | 2026-10-15 | **Rule:** `AppointmentService.changeableUntil` is the slot's date and start time less `app.appointments.change-cutoff-hours` (`APPOINTMENT_CHANGE_CUTOFF_HOURS`, default 2; negative counts as 0, and 0 means no cut-off). It is null for a visit that isn't upcoming. `requireBeforeCutoff` refuses a patient from that moment on ("now" from the `Clock`) with 409: "Less than 2 hours to go: this visit can't be cancelled or moved online any more. Please call the hospital at …". The number comes from `HOSPITAL_PHONE` and is left out when that isn't set. The check applies in `cancelAppointment` and, in `updateAppointmentById`, only when the slot changes. The front desk and admins (`isStaff`) are never limited. **For the app:** `AppointmentDTO.changeableUntil` is filled in every appointment the service returns: its own `toDTO` replaces the mapper calls. **Tests:** `AppointmentServiceTest` +5 with a frozen clock: at the deadline refused (slot kept), a minute before allowed; moving refused but the reason saved, with the deadline in the answer; the front desk at 08:55; a cut-off of 0; "1 hour" and no phone. New `CancellationCutoffH2Test` (shares FeatureFlowH2Test's context): the deadline in the patient's list; a visit moved to start in 1 hour can't be cancelled or moved (exact message), but its reason can change; the desk cancels it; a visit in 3 hours is cancelled, after which there is no deadline. **Mutation checks** (each makes a named test fail): the desk limited too, allowed at the deadline itself, moving not checked, cancelling not checked, the deadline an hour early, the deadline not sent to the app, a cut-off of 0 not off. Appointments README. No endpoint changed (snapshots as they were). Checker OK; full backend run green (198 tests, 1 skipped). |
| CC.2 My appointments | ✅ done | 2026-10-15 | **Upcoming visit cards:** before the deadline, "You can cancel or move it until 08:00 tomorrow" (`deadlineText`: today / tomorrow / a day and time). From the deadline, Cancel and Reschedule give way to "Less than 2 hours to go: to cancel or move it, please call +91 92895 20303" with a tap-to-call link (`HOSPITAL.phone`, `phoneHref`). The number of hours is worked out from the visit's start and the deadline, so it follows `APPOINTMENT_CHANGE_CUTOFF_HOURS`. A visit inside the cut-off keeps its Bill button; without a bill it has no action row. Without a deadline (cut-off 0) nothing changes. If the page was left open past the deadline, the server's refusal is shown. **Tests:** `AppointmentDetails.test.js` +3: the hint, the call note with the link, Bill kept, no Cancel / Reschedule; no deadline; the refusal after leaving the page open. `appointmentFormat.test.js` +1 (`deadlineText`). **Mutation checks** (fixed script, baseline first; each makes a named test fail): never locked, the deadline compared the wrong way, the cut-off in minutes, no until-when hint, tomorrow not named, the phone not tappable, the action row hidden when there is a bill. Screenshots: desktop, phone light and dark. 245 frontend tests; the build compiles without warnings. |
| CC.3 Docs, configuration and final check | ✅ done | 2026-10-15 | **Configuration:** `APPOINTMENT_CHANGE_CUTOFF_HOURS` in `docker-compose.yml` (`docker compose config` checked), `.env.docker.example`, the README's `.env` example and the environment table. **README:** the booking row in the features table. **Ideas doc** ([CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md)): the feature status (with PR #17), idea #9 done, the "partly done" line on cancel and reschedule removed, the suggested order, and a section "Cancellation cut-off". **Final check:** 198 backend tests (1 skipped), checker OK; 245 frontend tests, and the build compiles without warnings. |

## Follow-ups

- Booking a visit that starts within the cut-off is still allowed; the patient then can't cancel it online. Say so on the booking page if it causes questions.
