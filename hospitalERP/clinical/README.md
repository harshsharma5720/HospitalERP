# clinical (`hospital-clinical`)

Medical records: the doctor's consultation notes for an appointment (vitals, diagnosis, advice, follow-up), the e-prescription and its PDF, and a patient's medical history.

Records are visible only to the patient, their treating doctor(s) and admins — never to receptionists.

**Depends on:** common, identity, patients, staff, appointments · **Used by:** administration

## Public API — `com.itmonteur.hospitalerp.clinical`

| Class | Purpose |
|---|---|
| `Consultation`, `PrescriptionItem` | Consultation entity (one per appointment) and its prescribed medicines. |
| `ConsultationService` | `saveConsultation` (treating doctor only; marks the appointment completed), `getByAppointment`, `getForPrescription`, `getMyHistory`, `getPatientHistory`; `deleteAllForPatient/ForDoctor` for account deletion. |
| `ConsultationDTO`, `PrescriptionItemDTO` | Request/response objects. |

## Events

None published or consumed.

## Endpoints — `clinical.web`

`ConsultationController` under `/api/consultations`: save and read the consultation for an appointment, download `prescription.pdf`, `my` (a patient's history), `patient/{patientId}` (doctor/admin).

Access rules (`clinical.web.ClinicalSecurityRules`, a `ModuleSecurityRules` bean): `/api/consultations/**` patients, doctors and admins — never receptionists.

## Internal — `clinical.internal`

`ConsultationRepository`, `PrescriptionPdfService` (builds the PDF with OpenPDF).

## Configuration

| Env variable | Default | Meaning |
|---|---|---|
| `HOSPITAL_NAME` | `Hospital ERP` | Prescription letterhead. |
| `HOSPITAL_ADDRESS`, `HOSPITAL_PHONE` | empty | Prescription letterhead. |

## Tests

`ConsultationServiceTest`, `PrescriptionPdfServiceTest`; the full booking → consultation → PDF flow is in `FeatureFlowH2Test` (`app`).

Module diagram: [docs/modules/module-clinical.puml](../../docs/modules/module-clinical.puml).
