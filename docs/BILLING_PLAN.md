# Billing at the front desk — plan

**Branch:** `feature/billing` (from `main` = 73b1aec)
**Why:** the hospital records visits but not what they cost or whether they were paid. There are no fees, bills or receipts, and a visit cancelled after payment (e.g. by a doctor's leave) leaves no trace of the money owed back.
**Idea list:** [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md#not-implemented-yet) — "not implemented yet" #18 (first version: payment at the desk; online payment later).

## Decisions (2026-10-12)

| Question | Decision |
|---|---|
| How patients pay | **At the desk:** the receptionist records the payment (cash, card or UPI, with an optional reference such as the UPI transaction id) and prints a **PDF receipt**. No payment provider yet; online payment (Razorpay) can follow. |
| What a visit costs | **A consultation fee per doctor**, set by the admin. A bill starts with that fee; the front desk can **add extra items** (e.g. a dressing) and give a **discount**. |
| When the bill is made | **At check-in** (today's queue): the patient pays before seeing the doctor. Unpaid bills are listed for the front desk. |
| A paid visit that gets cancelled | **Refund due:** the bill says so; the front desk records the refund (cash, card or UPI) and the receipt shows it. |

## Design

- **A new `billing` module** (Maven module `hospital-billing`; tables `doctor_fees`, `bills`, `bill_items`, Flyway). It may use common, identity, patients, staff and appointments; nothing uses it except `app`. Appointments doesn't know about billing: it **publishes events** — checked in, left the queue (check-in undone, or rescheduled), cancelled — and billing listens inside the same transaction.
- **Fees:** one consultation fee per doctor (admin, `/api/admin/fees`). A doctor without a fee uses `BILLING_DEFAULT_FEE` (default 0); the fees page shows which doctors have none.
- **One bill per appointment** (`B-2026-000123`), made at check-in: the doctor's consultation fee as the first item (fixed), extra items and a discount while unpaid. Money is in rupees with 2 decimals.
- **Bill states:** *unpaid* → **paid** (method, reference, by whom, when); unpaid and the patient leaves the queue (check-in undone or the visit rescheduled) → *cancelled*, reopened when they check in again; a paid bill stays paid (it covers the visit when it happens). Cancelled visit: unpaid → *cancelled*, paid → **refund due** → *refunded* (method, reference, by whom, when).
- **Front desk** (`/api/receptionist/bills`, receptionists and admins): bills by date and status (unpaid / paid / refund due / all), paged; one bill; add / remove items, discount; pay; refund; receipt PDF (hospital letterhead from `HOSPITAL_*`, as on prescriptions).
- **Frontend:** admin **Fees** page; front-desk **Bills** page and bill page (pay, refund, receipt); the appointment list's Queue column links a checked-in visit to its bill.

## Steps

| Step | Work | Checked by |
|---|---|---|
| B.1 | **Billing module and fees:** Maven module, Flyway tables, entities, module rules; fees per doctor for the admin. | Fees end-to-end tests; MySQL migration test; module checker + Modulith; snapshots |
| B.2 | **Bills from the queue:** events from appointments (checked in, left the queue, cancelled); a bill at check-in with the consultation fee; cancel / reopen / refund due. | End-to-end tests (check-in → bill, undo / reschedule / cancel paid and unpaid, walk-in for today) |
| B.3 | **Front-desk billing API:** list, bill, items, discount, pay, refund, receipt PDF. | End-to-end tests (amounts, states, refused changes, access); PDF test; snapshots |
| B.4 | **Frontend, admin:** Fees page. | Frontend tests + build; screenshots |
| B.5 | **Frontend, front desk:** Bills page, bill page (items, discount, pay, refund, receipt), link from the appointment list. | Frontend tests + build; screenshots |
| B.6 | **Docs:** module READMEs, README, MySQL runbook (migration), idea list status. | — |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| B.0 Plan + decisions | ✅ done | 2026-10-12 | This file. |
| B.1 Billing module and fees | ✅ done | 2026-10-12 | New Maven module `hospital-billing` (`billing/pom.xml`: common, identity, patients, staff, appointments; in the root reactor after clinical, in `dependencyManagement`, a dependency of `app`); `tools/check_module_deps.py` (`billing` may use those five; `app` may use billing); Spring Modulith sees it (module docs regenerated — the other diagrams also pick up the queue feature's changes). Flyway `billing/V2026_10_12_1__billing.sql`: `doctor_fees`, `bills` (unique per appointment and per number, indexed by status + visit date), `bill_items` (cascade with the bill); no foreign keys to appointments / patients / doctors (financial records keep their names). Entities `DoctorFee`, `Bill` (`subtotal()`, `total()`, `addItem`), `BillItem`, `BillStatus`, `PaymentMethod` (enums as varchar, ids by identity, money `decimal(10,2)`), repositories. `FeeService` (`listFees` from `DoctorService.getAllDoctors` — admins see deactivated doctors, marked — with the fee or null and `defaultFee`; `setFee` 0 to 1,00,000, at most 2 decimals, records when and by whom; `feeFor(doctorId)` for B.2), `FeeController` `GET /api/admin/fees`, `PUT /api/admin/fees/{doctorId}` (admins only through `/api/admin/**`). `BILLING_DEFAULT_FEE` (default 0) in `application.properties`, `docker-compose.yml`, `.env.docker.example` and the README's `.env` example. Tests: new `BillingH2Test` (2: no fee → set 500 → "500.00", 750.5 → "750.50", 0 allowed, a deactivated doctor marked; -1 / 100000.01 / 12.345 / null → 400, 100000 allowed, unknown doctor 404, receptionist / doctor / patient 403), `DatabaseMigrationMySqlTest` (10 migrations; the billing tables exist and are empty; Hibernate validates the new entities on MySQL 8), `ModularityTest`. Mutation check: more than 2 decimals accepted → a test fails. Endpoint and access snapshots: +2 lines each (admins only). New `billing/README.md`; README (modules table, `.env` example, migrations table), MySQL runbook (10 migrations, 18 tables). Checker OK (12 modules), 166 backend tests (1 skipped). |
