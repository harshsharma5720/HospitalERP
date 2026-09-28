# appointments (`hospital-appointments`)

Booking: create, reschedule and cancel appointments (also for relatives), the lists for patients, doctors, receptionists and admins, and the day-before reminders. It decides *when* patients and doctors are notified.

**Depends on:** common, identity, notifications, patients, staff, scheduling · **Used by:** clinical, administration

## Public API — `com.itmonteur.hospitalerp.appointments`

| Class | Purpose |
|---|---|
| `Appointment`, `AppointmentStatus` | Appointment entity and its status (`SCHEDULED`, `CONFIRMED`, `COMPLETED`, `CANCELLED_BY_PATIENT`, `CANCELLED_BY_DOCTOR`). |
| `AppointmentService` | Booking (`createAppointment`, `updateAppointmentById`, `cancelAppointment`, `markAsCompleted`) and lists; for other modules: `findAppointmentEntity`, `markCompletedByConsultation` (clinical), `hasAppointment`, `findUpcomingForPatient/ForDoctor`, `deleteAllForPatient/ForDoctor` (administration), `notificationInfo`. |
| `AppointmentDTO` | Response object. |
| `AppointmentNotificationEvent` | "Notify about this appointment" (`BOOKED`, `CANCELLED`, `CANCELLED_BY_DOCTOR_LEAVE`, `REMINDER`). |

## Events

- **Publishes:** `AppointmentNotificationEvent(kind, info)` when an appointment is booked or cancelled (`AppointmentService`), cancelled by an approved leave (`AppointmentLeaveCanceller`) or due for a reminder (`AppointmentReminderService`). (Administration publishes it too, for the bookings removed when a doctor account is deleted.)
- **Listens to:**
  - `AppointmentNotificationEvent` — `AppointmentNotificationListener` sends SMS/email via notifications **only after the transaction commits** (`@TransactionalEventListener(AFTER_COMMIT)`), so a failed booking never sends a message.
  - `DoctorLeaveApprovedEvent` (staff) — `AppointmentLeaveCanceller` cancels active bookings in the leave dates.
  - `RelativeDeletedEvent` (patients) — `AppointmentRelativeUnlinker` keeps the bookings but removes the link to the relative.
  - `DoctorScheduleChangedEvent` (scheduling) — `ScheduleChangeSlotCleaner` keeps booked slots and lets scheduling remove the unused ones.

## Endpoints — `appointments.web`

| Controller | URL prefix |
|---|---|
| `AppointmentController` | `/appointment` (patient booking, cancel, lists) |
| `DoctorAppointmentController` | `/api/doctor` (a doctor's appointments, complete, counts) |
| `ReceptionistAppointmentController` | `/api/receptionist` (front-desk booking and lists) |

## Internal — `appointments.internal`

`AppointmentRepository`, `AppointmentMapper`, the four listeners above, and `AppointmentReminderService` — a scheduled job that sends the day-before reminders.

## Configuration

| Env variable | Default | Meaning |
|---|---|---|
| `REMINDERS_ENABLED` | `true` | Turn the reminder job on/off. |
| `REMINDERS_CRON` | `0 0 * * * *` (hourly) | When the job runs (Spring cron: second minute hour day month weekday). |

## Tests

`AppointmentServiceTest`, `AppointmentReminderServiceTest`; end-to-end booking in `FeatureFlowH2Test`, after-commit notifications in `NotificationsH2Test` (`app`).

Module diagram: [docs/modules/module-appointments.puml](../../docs/modules/module-appointments.puml).
