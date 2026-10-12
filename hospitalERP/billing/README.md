# billing (`hospital-billing`)

Billing at the front desk ([docs/BILLING_PLAN.md](../../docs/BILLING_PLAN.md)): a consultation fee per doctor, one bill per visit (made at check-in), payments and refunds recorded at the desk, PDF receipts.

**Depends on:** common, identity, patients, staff, appointments · **Used by:** app

## Public API — `com.itmonteur.hospitalerp.billing`

None: no other module uses billing. It learns about visits from the appointments module's events.

## Events

- **Listens to** (appointments; `BillingEventListener`, plain `@EventListener`s, so inside the appointment's transaction — a failed check-in leaves no bill, and a bill that can't be made fails the check-in):
  - `AppointmentCheckedInEvent` → `BillService.openBill`: the visit's bill (`B-2026-000123`, numbered from its id right after the insert) with one fixed line, "Consultation — Dr. …" at the doctor's fee (`FeeService.feeFor`, else the default); a cancelled bill is reopened, a paid one stays paid. One bill per appointment.
  - `AppointmentLeftQueueEvent` (check-in undone, or rescheduled) → `leftQueue`: an unpaid bill is cancelled.
  - `AppointmentCancelledEvent` (by the patient or the front desk, a doctor's approved leave, an account deactivation) → `visitCancelled`: unpaid → cancelled, paid → refund due.

## Endpoints — `billing.web`

| Controller | URL |
|---|---|
| `FeeController` | `GET /api/admin/fees` — every doctor (deactivated ones too, `active: false`) with `consultationFee` (null when none is set), by name, and `defaultFee`; `PUT /api/admin/fees/{doctorId}` `{"consultationFee": 500}` — 0 to 1,00,000 rupees, at most 2 decimals (400 otherwise, 404 for an unknown doctor). Admins only (`/api/admin/**`, administration's access rules). |

## Internal — `billing.internal`

`BillService`, `BillingEventListener` (above); `FeeService` (`listFees`, `setFee` — records when and by whom; `feeFor(doctorId)`: the doctor's fee or the default; `defaultFee()`), `DoctorFee` / `DoctorFeeRepository`, `DoctorFeeDTO`, `FeeListDTO`; `Bill` (one per appointment: number, patient, doctor, visit date, `BillStatus`, discount, items, payment and refund details; `subtotal()`, `total()`), `BillItem` (description, amount, `consultation` = the doctor's fee line), `BillStatus` (`UNPAID`, `PAID`, `CANCELLED`, `REFUND_DUE`, `REFUNDED`), `PaymentMethod` (`CASH`, `CARD`, `UPI`), `BillRepository`.

## Database

Flyway `billing/V2026_10_12_1__billing.sql`: `doctor_fees` (one row per doctor with a fee), `bills` (unique per `appointment_id` and per `bill_number`; indexed by status and visit date), `bill_items` (deleted with their bill). No foreign keys to appointments, patients or doctors: bills are financial records and keep the names they were made with. Amounts are `decimal(10,2)` rupees.

## Configuration

| Env variable | Default | Meaning |
|---|---|---|
| `BILLING_DEFAULT_FEE` | `0` | Consultation fee in rupees for doctors without their own. |

## Tests

End-to-end in `app`: `BillingH2Test` (fees: set, change, free, the highest, wrong amounts, unknown doctor, a deactivated doctor marked; receptionists, doctors and patients refused; bills: made at check-in with the fee or the default, one per visit, cancelled when the check-in is undone or the visit rescheduled and reopened at the next check-in, a paid one kept; a cancelled visit — by the front desk or a doctor's approved leave — cancels an unpaid bill and marks a paid one for refund); `DatabaseMigrationMySqlTest` (the tables on MySQL 8, empty after the upgrade).

Module diagram: [docs/modules/module-billing.puml](../../docs/modules/module-billing.puml).
