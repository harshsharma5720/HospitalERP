# appointments (`hospital-appointments`)

Booking: create, reschedule and cancel appointments (also for relatives), the lists for patients, doctors, receptionists and admins, and the day-before reminders. It decides *when* patients and doctors are notified.

**Depends on:** common, identity, notifications, patients, staff, scheduling · **Used by:** clinical, administration

## Public API — `com.itmonteur.hospitalerp.appointments`

| Class | Purpose |
|---|---|
| `Appointment`, `AppointmentStatus` | Appointment entity and its status (`SCHEDULED`, `CONFIRMED`, `COMPLETED`, `CANCELLED_BY_PATIENT`, `CANCELLED_BY_DOCTOR`). |
| `AppointmentService` | Booking (`createAppointment`, `updateAppointmentById`, `cancelAppointment`, `markAsCompleted`) and lists. **Cut-off** ([docs/CANCELLATION_CUTOFF_PLAN.md](../../docs/CANCELLATION_CUTOFF_PLAN.md)): from the visit's start (slot date and time) less `APPOINTMENT_CHANGE_CUTOFF_HOURS` on, a patient can't cancel or move it to another slot (409, with `HOSPITAL_PHONE` when set; the reason and details may still change); the front desk and admins can. `AppointmentDTO.changeableUntil` gives the patient's deadline (null when the visit isn't upcoming or there is no cut-off); "now" from the `Clock`; for other modules: `findAppointmentEntity`, `markCompletedByConsultation` (clinical), `hasAppointment`, `findUpcomingForPatient/ForDoctor`, `deleteAllForPatient/ForDoctor` (administration), `notificationInfo`; for account deactivation (administration): `cancelUpcomingForPatient/ForDoctor` (bookings from today on, slots freed, notified) and `hasAnyAppointmentForPatient/ForDoctor` (guards the permanent delete). |
| `AppointmentDTO` | Response object. |
| `AppointmentStatistics` | Admin dashboard figures by appointment date ([docs/ADMIN_DASHBOARD_PLAN.md](../../docs/ADMIN_DASHBOARD_PLAN.md)), all grouped count queries: `perDay(from, to)` → `DailyAppointmentCounts` (completed, open, cancelled by patient / by doctor; days without appointments as zeros), `busiestSpecializations` → `SpecializationCount`, `busiestDoctors` → `DoctorAppointmentCount` (cancelled ones not counted). "Completed" uses the module's usual rule: status `COMPLETED` or an old row with `is_completed` set. |
| `AppointmentNotificationEvent` | "Notify about this appointment" (`BOOKED`, `CANCELLED`, `CANCELLED_BY_DOCTOR_LEAVE`, `REMINDER`). |

## Events

- **Publishes** (for billing, [docs/BILLING_PLAN.md](../../docs/BILLING_PLAN.md); inside the transaction): `AppointmentCheckedInEvent(appointmentId)` (a check-in, once), `AppointmentLeftQueueEvent(appointmentId)` (a check-in undone, or a checked-in appointment rescheduled), `AppointmentCancelledEvent(appointmentId)` (every cancellation: patient / front desk, a doctor's approved leave, an account deactivation).
- **Publishes:** `AppointmentNotificationEvent(kind, info)` when an appointment is booked or cancelled (`AppointmentService`), cancelled by an approved leave (`AppointmentLeaveCanceller`) or due for a reminder (`AppointmentReminderService`). (Administration publishes it too, for the bookings removed when a doctor account is deleted.)
- **Listens to:**
  - `AppointmentNotificationEvent` — `AppointmentNotificationListener` queues the SMS/email in the notifications outbox **inside the same transaction** (`@EventListener`): they are stored exactly when the booking, cancellation or reminder is committed — a failed booking queues nothing — and sent right after the commit, with retries ([docs/RELIABLE_NOTIFICATIONS_PLAN.md](../../docs/RELIABLE_NOTIFICATIONS_PLAN.md)). The reminder flag and the reminder's messages are therefore saved together.
  - `DoctorLeaveApprovedEvent` (staff) — `AppointmentLeaveCanceller` cancels active bookings in the leave dates.
  - `RelativeDeletedEvent` (patients) — `AppointmentRelativeUnlinker` keeps the bookings but removes the link to the relative.
  - `DoctorScheduleChangedEvent` (scheduling) — `ScheduleChangeSlotCleaner` keeps booked slots and lets scheduling remove the unused ones.

## Endpoints — `appointments.web`

| Controller | URL prefix |
|---|---|
| `AppointmentController` | `/appointment` (patient booking, cancel, lists) |
| `DoctorAppointmentController` | `/api/doctor` (a doctor's appointments, complete, counts) |
| `DoctorQueueController` | `/api/doctor/queue` — the logged-in doctor's queue today ([docs/TODAYS_QUEUE_PLAN.md](../../docs/TODAYS_QUEUE_PLAN.md)): `GET` (with the doctor, waiting by time, not arrived, done), `POST /next` (the first waiting by time; 409 when nobody is waiting), `POST /{id}/call` (a particular waiting patient), `POST /{id}/not-here` (back to waiting). Only doctors: the URL also lets admins through, the service refuses them (they have no queue). |
| `QueueScreenController` | `GET /api/queue/screen` — the waiting-room screen: per doctor with a queue today, `doctorName`, `specialist`, `nowServing` (the last token called in) and `next` (up to 5 waiting tokens). No patient data, **no login needed**. |
| `ReceptionistAppointmentController` | `/api/receptionist` (front-desk booking and lists) |
| `FrontDeskController` | `/api/receptionist` — walk-in patients ([docs/WALK_IN_REGISTRATION_PLAN.md](../../docs/WALK_IN_REGISTRATION_PLAN.md)): `GET /patients?phone=` (patients with that number, recorded in the audit log), `GET /doctors/{doctorId}/next-free-slots?limit=5` (1-20, in time order), `POST /walk-in` (book for an existing patient, or register a new walk-in patient and book, in one step). Patient search and lists ([docs/PATIENT_SEARCH_PLAN.md](../../docs/PATIENT_SEARCH_PLAN.md)): `GET /patients/search?q=&page=&size=` (the one search box, a `PageResult`), `GET /patients/{patientId}` (one patient, audited as a profile view), `GET /appointments?from=&to=&doctorId=&status=&patient=&patientId=&order=&page=&size=` (every filter optional; a `PageResult` of `AppointmentDTO`, 20 per page by default, at most 100). Today's queue ([docs/TODAYS_QUEUE_PLAN.md](../../docs/TODAYS_QUEUE_PLAN.md)): `POST /appointments/{id}/check-in` (the patient has arrived: today's booked appointment gets its token; twice keeps the first), `DELETE /appointments/{id}/check-in` (undo a mistake; 409 once the doctor has called the patient in) |

Access rules (`appointments.web.AppointmentsSecurityRules`, a `ModuleSecurityRules` bean): the `/appointment` overview lists admins + receptionists, `getDoctorAppointments` doctors, `GET /api/queue/screen` everyone (no login), the rest of `/appointment/**` all roles (ownership is checked in `AppointmentService`). Everything under `/api/receptionist` is for receptionists and admins (staff's rules).

## Internal — `appointments.internal`

`AppointmentRepository`, `AppointmentMapper`, the four listeners above, and `AppointmentReminderService` — a scheduled job that sends the day-before reminders.

`FrontDeskService` (with `WalkInBookingRequest`, `WalkInBookingDTO`, `FreeSlotDTO`) — the front desk's walk-in patients. `registerAndBook` takes either `patientId` or `newPatient` (name, phone, gender; date of birth and email optional), plus `slotId`, `age` (0-130; needed when no date of birth is known) and an optional `message`. It is **one transaction**: the new patient is registered (`PtInfoService.registerWalkIn`) and the slot booked through `AppointmentService.createAppointment` — if the booking is refused (slot taken meanwhile → 409, doctor deactivated, ...), no patient record, audit entry or notification is left behind. The usual booking SMS / emails go to the phone (and email, if any) given. It also serves the patient search (`searchPatients`, `patient` — through `PtInfoService`) and the appointment list `listAppointments(AppointmentListQuery, page, size)`: `AppointmentFilters` turns the filters into a JPA `Specification` — dates (both included), doctor, status group (`upcoming` = scheduled / confirmed and not completed, `completed` = status `COMPLETED` or the old `is_completed` flag, `cancelled` = either cancellation, `all`), part of the patient's name on the appointment (typed `%` / `_` are plain characters) or one patient — sorted by date, start time and id (`order=asc` oldest first, the default; `desc` newest first). A visit whose slot row is gone is still listed (the sort uses an outer join). Wrong values (unknown status or order, start after end, page / size out of range) → 400. `AppointmentRepository` is also a `JpaSpecificationExecutor` for this. A walk-in booked for **today** is checked in straight away (`QueueService.checkIn`, same transaction).

`QueueService` — today's queue ([docs/TODAYS_QUEUE_PLAN.md](../../docs/TODAYS_QUEUE_PLAN.md)). `checkIn`: only today's active appointments; sets `checkedInAt` (to the microsecond, as the database keeps it) and `tokenNumber` = the appointment's place in the doctor's day (`SlotService.positionInDay`: its slot among all the doctor's slots that day, both shifts, in time order), kept from then on; if that number is already someone else's that day (slots changed after a schedule change), the next free number. `undoCheckIn` clears it unless `calledAt` is set. For the doctor (`currentDoctor`: the DOCTOR role and their profile, else 403): `myQueue`, `callNext`, `call` (own, today, booked, checked in; again changes nothing), `notHere` (clears `calledAt`); another doctor's patient → 403. `screen` groups everyone checked in today by doctor (`findCheckedInOn`). Queue times are kept to the microsecond, as the database keeps them, and a call is always later than the doctor's previous call that day (the system clock can be coarse: two calls within one tick still have an order, so the screen's "now serving" is clear). `Appointment` has `checkedInAt`, `tokenNumber`, `calledAt` (Flyway `appointments/V2026_10_11_1__appointments_queue.sql`; also in `AppointmentDTO`); rescheduling clears them (`clearQueueState`).

## Configuration

| Env variable | Default | Meaning |
|---|---|---|
| `REMINDERS_ENABLED` | `true` | Turn the reminder job on/off. |
| `REMINDERS_CRON` | `0 0 * * * *` (hourly) | When the job runs (Spring cron: second minute hour day month weekday). |
| `APPOINTMENT_CHANGE_CUTOFF_HOURS` | `2` | Hours before a visit from which patients can't cancel or move it themselves; `0`: no cut-off. |

## Tests

`AppointmentServiceTest` (also the cut-off with a frozen clock: to the minute, moving versus the reason, the front desk, 0, the wording), `AppointmentReminderServiceTest`; the cut-off end to end in `CancellationCutoffH2Test`; end-to-end booking in `FeatureFlowH2Test`, the front desk's walk-in booking in `WalkInPatientsH2Test`, patient search / details and the appointment list in `FrontDeskListsH2Test` (filters, order, paging, a missing slot, access), check-in, tokens, the doctor's queue and the screen in `QueueH2Test`, two calls within one clock tick in `QueueServiceTest`, walk-ins checked in for today in `FrontDeskServiceTest`, notifications through the outbox in `NotificationsH2Test` (`app`).

Module diagram: [docs/modules/module-appointments.puml](../../docs/modules/module-appointments.puml).
