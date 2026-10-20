# ABHA number on patient records — plan

Each person in India can have an **ABHA number** (Ayushman Bharat Health Account, 14 digits): their own health ID, also for children. The app keeps a record per patient and per family member, but has nowhere to put it, and the prescriptions and receipts it hands out can't carry it. This replaces the family view (idea #14 in [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md)): the user pointed out that every person, children included, has their own ABHA, so records are individual rather than one joint family account — the family members the app has stay for booking on behalf of someone without a phone.

Connecting to the national system itself (verifying a number by OTP, sharing records through ABDM) needs the hospital's registration with ABDM and is not part of this.

## Decisions (2026-10-20)

| Question | Decision |
|---|---|
| Who enters it | **The patient** in My account, for themself and each family member, and **the front desk** on the patient's page (also for the family members) and at walk-in registration. |
| What | **The ABHA number only**: 14 digits, shown as `12-3456-7890-1234`; entered with or without dashes or spaces; **optional** (not everyone has one). The ABHA address (e.g. `asha@abdm`) is left out. |
| Where it shows | On the patient's own pages and the desk's patient page, and **on the prescription PDF and the bill receipt**, under the patient's name (the family member's own number for a family member's visit). |
| One number, one person | **Refused** when it is already on another record — another patient or another family member, anywhere: "This ABHA number is already on another patient's record." |
| Way of working | Each finished, tested step is committed without asking; the branch is pushed at the end with the PR link. |

## Design

**Storing it** (patients module): `patient.abha_number` and `patient_relative.abha_number`, `varchar(14)`, the digits only, unique in each table (Flyway). One rule for both (`AbhaNumber`): spaces and dashes are dropped; empty means none; anything but 14 digits is refused ("An ABHA number has 14 digits"). Before saving, both tables are checked, so a number can't sit on a patient and a family member at once; the unique keys catch two saves at the same moment. Changing it is in the audit log like the other profile fields (the field's name, never the number).

**Entering it:**
- The patient: My account's details (`abhaNumber` in the profile update; an empty value removes it) and each family member's form.
- The front desk: `PUT /api/receptionist/patients/{patientId}/abha` and `PUT /api/receptionist/patients/{patientId}/relatives/{relativeId}/abha` (`{abhaNumber}`), and the family members on the patient's page (`GET /api/receptionist/patients/{patientId}/relatives`); the walk-in form's new patient can bring one.

**Showing it:** the patient's DTOs carry `abhaNumber` (digits; the app writes the dashes). The prescription PDF (clinical) and the receipt (billing) add "ABHA 12-3456-7890-1234" under the patient's name when there is one — the family member's for a family member's visit, read when the PDF is made, so a number added later shows on a receipt printed later.

## Steps

| Step | Work | Checked by |
|---|---|---|
| AB.1 | **Storing and entering it (backend):** the columns (Flyway), `AbhaNumber`, the patient's and family members' edits, the desk's endpoints, walk-in registration, the one-number-one-person check. | Unit tests; end-to-end tests (formats, a duplicate on a patient and on a family member, clearing it, the desk, walk-ins, access, audit); mutation checks |
| AB.2 | **On the prescription and the receipt (backend).** | Tests on the PDFs' text; mutation checks |
| AB.3 | **The app (frontend):** My account and the family member form, the walk-in form, the desk's patient page with the family members. | Tests; mutation checks; screenshots |
| AB.4 | **Docs** (README, module READMEs, the end-to-end test guide, the ideas doc, the MySQL runbook — also its first real MySQL run since the safer logins), final check, push and PR link. | Full backend and frontend runs; build |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| AB.0 Plan + decisions | ✅ done | 2026-10-20 | This file. The pending `DatabaseMigrationMySqlTest` ran on 2026-10-20 (Docker answered again): 4 of 4, the upgrade (14 migrations) and a fresh database (15) to version 2026.10.19.2 — the safer logins, cash closes and inbox migrations included. |
| AB.1 Storing and entering it | ✅ done | 2026-10-20 | Flyway `patients/V2026_10_20_1__patients_abha_number.sql` (both columns, a unique key on each); `AbhaNumber` (normalize / format), `internal.AbhaRegistry` (one number, one person, across both tables, 409); the patient's profile update (not sent: unchanged, empty: removed) and the family member's add / update; `PatientDeskController` — `PUT /abha`, `GET /relatives`, `PUT /relatives/{relativeId}/abha` (receptionists + admins, staff's rule; another patient's family member → 404); walk-ins (a taken number books nothing). Audited as `PATIENT_PROFILE_UPDATED` ("changed: ABHA number", "changed: a family member's ABHA number"). `AbhaNumberTest` (3), `AbhaNumberH2Test` (2: the patient's and family members' numbers, duplicates both ways, re-saving one's own, clearing it frees it; the desk, its audit entries, 404s, access, an admin; walk-ins); `DatabaseMigrationMySqlTest` checks the new migration. Endpoint + access snapshots: the 3 new URLs. Mutation checks: 24 of 24 killed. |
| AB.2 On the prescription and the receipt | ✅ done | 2026-10-20 | `Appointment.abhaNumber()`: whose number a visit shows, as it is when the PDF is made — a family member's own (or none: never the patient's), the patient's for a visit under the patient's own name (case and spaces aside), and none otherwise (a name typed at booking, or a family member since removed, is someone else). `PrescriptionPdfService` adds "ABHA 12-3456-7890-1234" under the patient's details; `ReceiptPdfService.render(bill, abhaNumber)` under the patient's name, `BillDeskService.receiptOf` looking it up through the bill's appointment (the desk's and the patient's receipts). `AppointmentAbhaNumberTest` (3), `PrescriptionPdfServiceTest` and `ReceiptPdfServiceTest` (read back as text), `AbhaNumberPdfH2Test` (a family member's, the patient's, none for a family member without one, a number added later on later prints, a family member removed). Mutation checks: 9 of 9 killed. |
| AB.3 The app (frontend) | ✅ done | 2026-10-20 | `shared/utils/abha.js` (one rule: 14 digits or none, written 12-3456-7890-1234; the inputs get the digit keypad, a pattern and a placeholder). **My account:** "ABHA number" in the details (emptied: removed), each family member's in the list; **the family member form:** their own number; **Walk-in:** an optional ABHA number for a new patient, checked before sending — a refused booking now keeps the chosen time when it is still free (it used to jump to the earliest). **The desk's patient page:** the ABHA number changed in place (`AbhaEditor`: checked, the server's reason shown, empty removes it) and a new **Family members** card with each member's number. Found on the way: `Field`'s hint and error weren't tied to the control, so screen readers never read them — now its `aria-describedby` (every form with a hint). Tests: 305 (all frontend), the ABHA ones in `abha.test.js`, MyAccount, AddRelativePage, WalkInBooking, FrontDeskPatient and the `Field` test. Mutation checks: 34 of 34 killed. Screenshots: the desk's page (light, dark, editing, phone), My account (editing, phone), the family member form, the walk-in form; the prescription and the receipt with an ABHA number, rendered. |
