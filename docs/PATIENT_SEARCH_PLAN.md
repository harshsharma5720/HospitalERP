# Patient search and paged front-desk lists — plan

**Branch:** `feature/patient-search` (from `main` = 53c85d0)
**Why:** the front desk can only find a patient by phone (walk-in page), there is no patient list to browse, the admin sidebar's "Manage Patients" button leads nowhere, and the receptionist dashboard loads *every* appointment at once ("Load All Appointments", search by doctor name or doctor user id). All of this gets slower and less usable as the hospital's data grows.
**Idea list:** [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md#not-implemented-yet) — "not implemented yet" #12.

## Decisions (2026-10-09)

| Question | Decision |
|---|---|
| Which lists | **A Patients page** (front desk and admins; also behind the admin sidebar's "Manage Patients") and **the front-desk appointment list** (filters: date, doctor, status, patient), both paged. Not now: the admin users list and the doctor portal's lists. |
| How a patient is searched | **One box:** part of a name, a phone number or a patient ID; the server works out which one it is. |
| Opening a patient | **Details + appointments:** contact details, upcoming and past appointments, and a **Book** button that opens Walk-in booking with this patient chosen. |

## Design

- **One box** (`PtInfoService.searchPatients(query, page, size)`, patients module):
  - `#41` or a number with up to 9 digits → **patient ID**;
  - something phone-like (digits with `+`, spaces, dashes, brackets) → **phone**, the same rule as the walk-in search (at least 10 digits, the last 10 compared);
  - anything else → **part of the name**, case does not matter;
  - empty → **all patients**.
  Sorted by name, 20 per page (at most 100). Every search goes into the audit log like the phone search (`PATIENTS_SEARCHED`: "by name "meer", 12 found", "by patient ID 41, 1 found" — linked to that patient); browsing without a search is `PATIENT_LIST_VIEWED` ("page 2: 20 of 523 patients").
- **Pages** look the same everywhere: `{entries, page, size, totalEntries, totalPages}` (a shared `PageResult` in common; the audit log and notifications pages already use this shape).
- **Front-desk endpoints** (appointments module, under `/api/receptionist/` — receptionists and admins):
  - `GET /patients/search?q=&page=&size=` — the one-box search;
  - `GET /patients/{patientId}` — one patient's details (recorded as a profile view in the audit log);
  - `GET /appointments?from=&to=&doctorId=&status=&patient=&patientId=&order=&page=&size=` — the appointment list: dates (both included), doctor, status group (**upcoming**, **completed**, **cancelled**, or all), part of the patient's name or one patient; oldest first by date and time (newest first with `order=desc`, for a patient's history). The old list endpoints stay for now.
- **Frontend:**
  - **Patients** page (`/patients` for receptionists, `/admin/patients` for admins): search box, paged results (name, gender, age, phone, app account / front-desk record) → **patient page**: details, upcoming appointments, past appointments (paged), **Book** → Walk-in booking with the patient chosen.
  - **Front-desk appointments** (`/receptionist-appointments`, replacing the old dashboard): today by default; date range, doctor, status and patient filters; paged table with a link to each patient.

## Steps

| Step | Work | Checked by |
|---|---|---|
| P.1 | **Patient search:** `PageResult`, `PtInfoService.searchPatients` (one box, paged, audited), name query. | Service tests (which search each input becomes, audit details); H2 test of the name query (case, wildcards typed by the user, paging, order) |
| P.2 | **Front-desk API:** patient search, one patient's details, the filtered and paged appointment list. | End-to-end tests (search forms, paging, filters, order, access); endpoint and access snapshots updated on purpose |
| P.3 | **Frontend: Patients page and patient page** (receptionists and admins), Book → Walk-in with the patient chosen. | Frontend tests + build; screenshots |
| P.4 | **Frontend: front-desk appointments** replacing the old receptionist dashboard. | Frontend tests + build; screenshots |
| P.5 | **Docs:** module READMEs, README, idea list status. | — |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| P.0 Plan + decisions | ✅ done | 2026-10-09 | This file. |
| P.1 Patient search | ✅ done | 2026-10-09 | `common.PageResult<T>` (`entries, page, size, totalEntries, totalPages`; `of(...)` works out the page count). `PtInfoService.searchPatients(query, page, size)`: page from 0, 1-100 per page (checked first); spaces collapsed; `#41` / up to 9 digits → patient ID (`findById`); phone-like (digits with `+`, spaces, dashes, brackets) → the walk-in phone rule (at least 10 digits, the last 10 compared; now shared with `findByPhone`), sorted by name and paged in memory; anything else → `PtInfoRepository.searchByName`: `LOWER(patientName) LIKE :pattern ESCAPE '!'` with the typed `!`, `%`, `_` escaped (not a backslash: MySQL would read it as a string escape); empty → all. Sorted by name, then id. Audit: `PATIENTS_SEARCHED` "by name "MEERA Nair", 1 found" (the term as typed, cut at 50 characters), "by patient ID 41, 1 found" (linked to the patient found), "by phone ...1111, 2 found"; browsing: `PATIENT_LIST_VIEWED` "page 2 of 2, 21 patients". Tests: `PtInfoServiceSearchTest` (5, patients module's first test: each input form, audit details, page checks before any query), `PatientSearchH2Test` (3, shares FeatureFlowH2Test's context: case, typed `_` / `%`, 23 names over 3 pages in order, ID and phone on real data with the audit link, browsing everyone without overlap), `DatabaseMigrationMySqlTest.patientNameSearchRunsOnMySql` (the query on MySQL 8). Mutation checks: without the escaping, or without `LOWER`, a test fails. Found on the way: VS Code compiled classes against the old module jars in `~/.m2` (from before the walk-in feature), so Maven ran IDE-built classes with "Unresolved compilation problems" — fixed by `mvnw clean install -DskipTests`. Common and patients READMEs. Checker OK, 151 backend tests (1 skipped). |
