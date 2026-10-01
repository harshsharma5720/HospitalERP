# patients (`hospital-patients`)

Patient profiles and their relatives (people a patient can book appointments for).

**Depends on:** common, identity · **Used by:** appointments, clinical, administration

## Public API — `com.itmonteur.hospitalerp.patients`

| Class | Purpose |
|---|---|
| `PtInfo`, `PtRelative`, `RelationShip` | Entities for the patient profile (linked to its `User`) and relatives, and the relationship enum. |
| `PtInfoService` | Profile read/update (`getAllPtInfo`, `getPtInfoById`, `updatePtInfoById`) plus what other modules need: `findPatientEntity`, `findPatientEntityByUserId`, `deletePatientEntity`. |
| `PtRelativeService` | Add/list/update/delete relatives; `findRelativeEntity` for appointments. |
| `PtInfoDTO`, `PtRelativeDTO`, `PatientMapper` | Response objects and entity → DTO mapping (also used by administration). |
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

No unit tests of its own; covered by `ProfileCreatorsTest` and the end-to-end tests in `app`.

Module diagram: [docs/modules/module-patients.puml](../../docs/modules/module-patients.puml).
