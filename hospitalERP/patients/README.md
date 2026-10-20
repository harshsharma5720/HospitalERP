# patients (`hospital-patients`)

Patient profiles and their relatives (people a patient can book appointments for).

**Depends on:** common, identity, audit · **Used by:** appointments, clinical, administration

## Public API — `com.itmonteur.hospitalerp.patients`

| Class | Purpose |
|---|---|
| `PtInfo`, `PtRelative`, `RelationShip` | Entities for the patient profile (linked to its `User` — or without one: a **walk-in record** registered at the front desk) and relatives, and the relationship enum. `email` is optional (walk-ins), `createdAt` is set for every new record. |
| `PtInfoService` | Profile read/update (`getAllPtInfo`, `getPtInfoById`, `updatePtInfoById`) plus what other modules need: `findPatientEntity`, `findPatientEntityByUserId`, `findPatientNames` (one query, for the audit log page), `deletePatientEntity`. Views, updates (with the names of the changed fields, never their values) and the all-patients list go into the [audit log](../audit/README.md); a refused view records nothing. |
| `PtInfoService` (front desk) | `registerWalkIn(WalkInPatient)` — a record without a login: name, phone (10–14 digits, stored without spaces / dashes), gender required; date of birth and email optional ([docs/WALK_IN_REGISTRATION_PLAN.md](../../docs/WALK_IN_REGISTRATION_PLAN.md)); `findByPhone(phone)` — every patient whose number ends with the same 10 digits (a family may share one), each marked `hasLogin`; both recorded in the audit log (`WALK_IN_REGISTERED`, `PATIENTS_SEARCHED`). For the dashboard: `countWalkInPatients`, `countNewWalkInsPerDay`, `firstWalkInCreationTime`. |
| `PtInfoService` (patient search) | `searchPatients(query, page, size)` — the front desk's one search box ([docs/PATIENT_SEARCH_PLAN.md](../../docs/PATIENT_SEARCH_PLAN.md)): `#41` or a number of up to 9 digits is a **patient ID**; something phone-like is a **phone number** (the same rule as `findByPhone`); anything else is **part of the name** (case does not matter; `%` and `_` typed in the box are plain characters); empty lists **everyone**. Sorted by name, one `PageResult` page (from 0, at most 100 per page). Searches are audited as `PATIENTS_SEARCHED` (`by name "meer", 12 found`; an ID search is linked to the patient found), browsing as `PATIENT_LIST_VIEWED` (`page 2 of 27, 523 patients`). `getPatient(patientId)` — one patient for the front desk (access is checked on its URL), audited as `PATIENT_PROFILE_VIEWED`. |
| `PtInfoService` / `PtRelativeService` (ABHA number) | The **ABHA number** of a patient and of each family member ([docs/ABHA_NUMBER_PLAN.md](../../docs/ABHA_NUMBER_PLAN.md)): optional, 14 digits, stored without dashes or spaces (`AbhaNumber.normalize`; `AbhaNumber.format` writes `12-3456-7890-1234`). Set by the patient (`abhaNumber` in the profile update — not sent: unchanged, empty: removed — and in each family member's form) and by the front desk (`PtInfoService.setAbhaNumber`, `PtRelativeService.setAbhaNumber` — the family member must be the patient's — and walk-in registration). **One number, one person:** `internal.AbhaRegistry` refuses (409) a number already on another patient or family member; each table's unique key catches two saves at the same moment. Changes are audited as `PATIENT_PROFILE_UPDATED` (the field's name, never the number). `relativesForDesk(patientId)` lists a patient's family members for the desk. |
| `AbhaNumber` | The ABHA number's one rule: `normalize` (14 digits or none; anything else is a 400) and `format`. |
| `WalkInPatient` | The details of a walk-in registration (with an optional ABHA number). |
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
| `PatientDeskController` | `/api/receptionist/patients/{patientId}` — `PUT /abha`, `GET /relatives`, `PUT /relatives/{relativeId}/abha` (the front desk; `{abhaNumber}`, empty removes it) |

Closing a patient account (`DELETE /api/patient/deleteAccount/{ptId}`, the patient themself or an admin) lives in administration, because it spans several modules. Despite the old name it **deactivates**: the profile and all history are kept, and only an admin can reactivate. `deletePatientEntity` is only used by the admin's permanent delete, which is refused while the patient has appointments ([docs/ACCOUNT_DEACTIVATION_PLAN.md](../../docs/ACCOUNT_DEACTIVATION_PLAN.md)).

Access rules (`patients.web.PatientsSecurityRules`, a `ModuleSecurityRules` bean): `/api/patient/getAll` admins + receptionists; the rest of `/api/patient/**` patients + admins. `PatientDeskController` is under `/api/receptionist/**`, staff's rule: receptionists + admins.

## Internal — `patients.internal`

`PtInfoRepository`, `PtRelativeRepository`, `PatientProfileCreator`, `AbhaRegistry` (the one-number-one-person check across both tables).

## Configuration

None.

## Tests

`PtInfoServiceSearchTest` (which search each input becomes, audit details, page checks), `AbhaNumberTest` (the ABHA number's formats). Also covered by `ProfileCreatorsTest` and the tests in `app`: `patientProfileAccessIsAudited` (audit entries), `WalkInPatientsH2Test`, `PatientSearchH2Test` (the name query: case, typed wildcards, paging, order; ID and phone on real data), `AbhaNumberH2Test` (the patient's and the desk's edits, family members, duplicates, walk-ins, access, audit) and `DatabaseMigrationMySqlTest` (the name query on MySQL).

Module diagram: [docs/modules/module-patients.puml](../../docs/modules/module-patients.puml).
