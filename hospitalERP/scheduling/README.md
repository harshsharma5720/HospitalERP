# scheduling (`hospital-scheduling`)

When doctors can be booked: each doctor's weekly working hours and slot length, and the bookable slots generated from them.

**Depends on:** common, identity, staff · **Used by:** appointments, administration

## Public API — `com.itmonteur.hospitalerp.scheduling`

| Class | Purpose |
|---|---|
| `DoctorSchedule`, `Slot`, `Shift` | Entities for a doctor's weekly schedule and a bookable time slot, and the `MORNING` / `EVENING` shift enum. |
| `SlotService` | `generateSlots`, `getAvailableSlots`, `nextFreeSlots` (a doctor's next free slots from now on, in time order, at most N - for the front desk), `lockAndBook` (pessimistic lock so a slot can't be double-booked), `releaseSlot`, `deleteUnusedSlots`, `deleteAllForDoctor`, `hoursFor`. A deactivated doctor has no available slots and `lockAndBook` refuses their slots (409). |
| `DoctorScheduleService` | `getSchedule`, `updateSchedule`, `deleteAllForDoctor`. |
| `DoctorScheduleDTO` | Request/response object for a weekly schedule. |
| `DoctorScheduleChangedEvent` | Published when a schedule is changed. |

## Events

- **Publishes:** `DoctorScheduleChangedEvent(Long doctorId)` — appointments looks up which future slots are still booked and calls `SlotService.deleteUnusedSlots`, so booked slots survive and only unused ones are removed.
- **Listens to:** `DoctorLeaveApprovedEvent` (staff) — `SlotLeaveBlocker` marks the doctor's slots in the leave dates as unavailable.

## Endpoints — `scheduling.web`

| Controller | URL prefix |
|---|---|
| `SlotController` | `/api/slots` (`available/{doctorId}`, `generate/{doctorId}`) |
| `DoctorScheduleController` | `/api/doctor` (weekly schedule endpoints) |

Access rules (`scheduling.web.SchedulingSecurityRules`, a `ModuleSecurityRules` bean): `/api/slots/generate/**` admins; the rest of `/api/slots/**` any logged-in user.

## Internal — `scheduling.internal`

`SlotRepository`, `DoctorScheduleRepository`, `ScheduleDefaults` (default hours and slot length when a doctor has no schedule yet), `SlotLeaveBlocker`.

## Configuration

None.

## Tests

`DoctorScheduleValidationTest`, `SlotServiceScheduleTest`, `SlotServiceNextFreeSlotsTest`; the leave listener is covered by `DoctorLeaveListenersTest` (`app`).

Module diagram: [docs/modules/module-scheduling.puml](../../docs/modules/module-scheduling.puml).
