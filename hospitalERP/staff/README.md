# staff (`hospital-staff`)

Hospital staff: doctor and receptionist profiles, and leave requests. This is where HR features will grow.

**Depends on:** common, identity · **Used by:** scheduling, appointments, clinical, administration

## Public API — `com.itmonteur.hospitalerp.staff`

| Class | Purpose |
|---|---|
| `Doctor`, `Receptionist`, `Specialist` | Profile entities (linked to their `User`) and the doctor speciality enum. |
| `LeaveRequest`, `LeaveStatus` | Leave entity and its status (`PENDING`, `APPROVED`, `REJECTED`). |
| `DoctorService` | Doctor profiles and directory (`getAllDoctors`, `findDoctorsBySpecialization`, `updateDoctor`, ...) plus `findDoctorEntity`, `findDoctorEntityByUserId`, `deleteDoctorEntity` for other modules. |
| `ReceptionistService` | Receptionist profiles, plus `find…Entity` / `deleteReceptionistEntity`. |
| `LeaveRequestService` | Apply, list, update and decide leaves (`updateLeaveStatus`), `isOnApprovedLeave` (used by scheduling), `deleteAllForUser`. |
| `DoctorDTO`, `ReceptionistDTO`, `LeaveRequestDTO`, `DoctorMapper` | Response objects and mapping. |
| `DoctorLeaveApprovedEvent` | Published when a doctor's leave is approved. |

## Events

- **Publishes:** `DoctorLeaveApprovedEvent(doctorId, startDate, endDate)`, inside the approval transaction. Scheduling blocks the slots and appointments cancels the bookings in those dates — all in one transaction.
- **Listens to:** `UserRegisteredEvent` (identity) — `StaffProfileCreator` creates the doctor or receptionist profile for a new staff account.

## Endpoints — `staff.web`

| Controller | URL prefix |
|---|---|
| `DoctorController` | `/api/doctor` (profile) |
| `DoctorDirectoryController` | `/api/patient` (public doctor list for patients) |
| `ReceptionistController` | `/api/receptionist` |
| `LeaveRequestController` | `/api/leaves` |

Other controllers share some of these prefixes (for example `/api/doctor` schedule and appointment endpoints live in scheduling and appointments). Deleting a doctor or receptionist lives in administration.

Access rules (`staff.web.StaffSecurityRules`, a `ModuleSecurityRules` bean): the doctor directory is public; `/api/doctor/**` doctors + admins, `/api/receptionist/**` receptionists + admins, `/api/leaves/**` staff.

## Internal — `staff.internal`

`DoctorRepository`, `ReceptionistRepository`, `LeaveRequestRepository`, `StaffProfileCreator`.

## Configuration

None.

## Tests

`LeaveRequestServiceTest`; cross-module parts in `ProfileCreatorsTest` and `DoctorLeaveListenersTest` (`app`).

Module diagram: [docs/modules/module-staff.puml](../../docs/modules/module-staff.puml).
