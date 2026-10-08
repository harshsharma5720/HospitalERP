# patients (`hospital-patients`)

Patient profiles and their relatives (people a patient can book appointments for).

**Depends on:** common, identity, audit · **Used by:** appointments, clinical, administration

## Public API — `com.itmonteur.hospitalerp.patients`

| Class | Purpose |
|---|---|
| `PtInfo`, `PtRelative`, `RelationShip` | Entities for the patient profile (linked to its `User` — or without one: a **walk-in record** registered at the front desk) and relatives, and the relationship enum. `email` is optional (walk-ins), `createdAt` is set for every new record. |
| `PtInfoService` | Profile read/update (`getAllPtInfo`, `getPtInfoById`, `updatePtInfoById`) plus what other modules need: `findPatientEntity`, `findPatientEntityByUserId`, `findPatientNames` (one query, for the audit log page), `deletePatientEntity`. Views, updates (with the names of the changed fields, never their values) and the all-patients list go into the [audit log](../audit/README.md); a refused view records nothing. |
| `PtInfoService` (front desk) | `registerWalkIn(WalkInPatient)` — a record without a login: name, phone (10–14 digits, stored without spaces / dashes), gender required; date of birth and email optional ([docs/WALK_IN_REGISTRATION_PLAN.md](../../docs/WALK_IN_REGISTRATION_PLAN.md)); `findByPhone(phone)` — every patient whose number ends with the same 10 digits (a family may share one), each marked `hasLogin`; both recorded in the audit log (`WALK_IN_REGISTERED`, `PATIENTS_SEARCHED`). For the dashboard: `countWalkInPatients`, `countNewWalkInsPerDay`, `firstWalkInCreationTime`. |
| `WalkInPatient` | The details of a walk-in registration. |
| `PtRelativeService` | Add/list/update/delete relatives; `findRelativeEntity` for appointments. |
| `PtInfoDTO`, `PtRelativeDTO`, `PatientMapper` | Response objects and entity → DTO mapping (also used by administration); `PtInfoDTO.hasLogin` is false for a walk-in record. |
| `RelativeDeletedEvent` | Published when a relative is deleted. |

## Events

- **Publishes:** `RelativeDeletedEvent(Long relativeId)` — appointments unlinks the relative from its bookings (the bookings are kept).
- **Listens to:** `UserRegisteredEvent` (identity) — `PatientProfileCreator` creates the patient profile for a new `PATIENT` account, in the same transaction.

## Endpoints — `patients.web`

| Controller | URL prefix |
|---|---|
| `PtInfoController` | `/api/patient` (profile) |
| `PtRelativeController` | `/api/patient/relative` |

Closing a patient account (`DELETE /api/patient/deleteAccount/{ptId}`, the patient themself or an admin) lives in administration, because it spans several modules. Despite the old name it **deactivates**: the profile and all history are kept, and only an admin can reactivate. `deletePatientEntity` is only used by the admin's permanent delete, which is refused while the patient has appointments ([docs/ACCOUNT_DEACTIVATION_PLAN.md](../../docs/ACCOUNT_DEACTIVATION_PLAN.md)).

Access rules (`patients.web.PatientsSecurityRules`, a `ModuleSecurityRules` bean): `/api/patient/getAll` admins + receptionists; the rest of `/api/patient/**` patients + admins.

## Internal — `patients.internal`

`PtInfoRepository`, `PtRelativeRepository`, `PatientProfileCreator`.

## Configuration

None.

## Tests

No unit tests of its own; covered by `ProfileCreatorsTest` and the end-to-end tests in `app` (`patientProfileAccessIsAudited` for the audit entries).

Module diagram: [docs/modules/module-patients.puml](../../docs/modules/module-patients.puml).
