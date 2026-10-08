# appointments (`hospital-appointments`)

Booking: create, reschedule and cancel appointments (also for relatives), the lists for patients, doctors, receptionists and admins, and the day-before reminders. It decides *when* patients and doctors are notified.

**Depends on:** common, identity, notifications, patients, staff, scheduling · **Used by:** clinical, administration

## Public API — `com.itmonteur.hospitalerp.appointments`

| Class | Purpose |
|---|---|
| `Appointment`, `AppointmentStatus` | Appointment entity and its status (`SCHEDULED`, `CONFIRMED`, `COMPLETED`, `CANCELLED_BY_PATIENT`, `CANCELLED_BY_DOCTOR`). |
| `AppointmentService` | Booking (`createAppointment`, `updateAppointmentById`, `cancelAppointment`, `markAsCompleted`) and lists; for other modules: `findAppointmentEntity`, `markCompletedByConsultation` (clinical), `hasAppointment`, `findUpcomingForPatient/ForDoctor`, `deleteAllForPatient/ForDoctor` (administration), `notificationInfo`; for account deactivation (administration): `cancelUpcomingForPatient/ForDoctor` (bookings from today on, slots freed, notified) and `hasAnyAppointmentForPatient/ForDoctor` (guards the permanent delete). |
| `AppointmentDTO` | Response object. |
| `AppointmentStatistics` | Admin dashboard figures by appointment date ([docs/ADMIN_DASHBOARD_PLAN.md](../../docs/ADMIN_DASHBOARD_PLAN.md)), all grouped count queries: `perDay(from, to)` → `DailyAppointmentCounts` (completed, open, cancelled by patient / by doctor; days without appointments as zeros), `busiestSpecializations` → `SpecializationCount`, `busiestDoctors` → `DoctorAppointmentCount` (cancelled ones not counted). "Completed" uses the module's usual rule: status `COMPLETED` or an old row with `is_completed` set. |
| `AppointmentNotificationEvent` | "Notify about this appointment" (`BOOKED`, `CANCELLED`, `CANCELLED_BY_DOCTOR_LEAVE`, `REMINDER`). |

## Events

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
| `ReceptionistAppointmentController` | `/api/receptionist` (front-desk booking and lists) |
| `FrontDeskController` | `/api/receptionist` — walk-in patients ([docs/WALK_IN_REGISTRATION_PLAN.md](../../docs/WALK_IN_REGISTRATION_PLAN.md)): `GET /patients?phone=` (patients with that number, recorded in the audit log), `GET /doctors/{doctorId}/next-free-slots?limit=5` (1-20, in time order), `POST /walk-in` (book for an existing patient, or register a new walk-in patient and book, in one step) |

Access rules (`appointments.web.AppointmentsSecurityRules`, a `ModuleSecurityRules` bean): the `/appointment` overview lists admins + receptionists, `getDoctorAppointments` doctors, the rest of `/appointment/**` all roles (ownership is checked in `AppointmentService`). Everything under `/api/receptionist` is for receptionists and admins (staff's rules).

## Internal — `appointments.internal`

`AppointmentRepository`, `AppointmentMapper`, the four listeners above, and `AppointmentReminderService` — a scheduled job that sends the day-before reminders.

`FrontDeskService` (with `WalkInBookingRequest`, `WalkInBookingDTO`, `FreeSlotDTO`) — the front desk's walk-in patients. `registerAndBook` takes either `patientId` or `newPatient` (name, phone, gender; date of birth and email optional), plus `slotId`, `age` (0-130; needed when no date of birth is known) and an optional `message`. It is **one transaction**: the new patient is registered (`PtInfoService.registerWalkIn`) and the slot booked through `AppointmentService.createAppointment` — if the booking is refused (slot taken meanwhile → 409, doctor deactivated, ...), no patient record, audit entry or notification is left behind. The usual booking SMS / emails go to the phone (and email, if any) given.

## Configuration

| Env variable | Default | Meaning |
|---|---|---|
| `REMINDERS_ENABLED` | `true` | Turn the reminder job on/off. |
| `REMINDERS_CRON` | `0 0 * * * *` (hourly) | When the job runs (Spring cron: second minute hour day month weekday). |

## Tests

`AppointmentServiceTest`, `AppointmentReminderServiceTest`; end-to-end booking in `FeatureFlowH2Test`, the front desk's walk-in booking in `WalkInPatientsH2Test`, notifications through the outbox in `NotificationsH2Test` (`app`).

Module diagram: [docs/modules/module-appointments.puml](../../docs/modules/module-appointments.puml).
