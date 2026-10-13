# Patient "My bills" — plan

Billing ([BILLING_PLAN.md](BILLING_PLAN.md)) gives every visit a bill at check-in, with payment, refund and a PDF receipt, but only the front desk can see it: every bill endpoint is under `/api/receptionist/bills`, and patients get 403. A patient who wants a receipt has to ask at the desk. This piece of work lets patients see their own bills, including their family members' visits, and download the receipts in the app. It is read-only: patients still pay at the desk (online payment with Razorpay is idea #18).

## Decisions (2026-10-13)

| Question | Decision |
|---|---|
| Where patients see their bills | **Both:** a **Bill** button on each visit that has one in My appointments, which opens the bill, and a **Bills** section in My account listing every bill (status, total, Download receipt). |
| Way of working | Each finished, tested step is committed without asking; the branch is pushed at the end with the PR link. |

## Design

**Backend** (billing module):

- **Endpoints** `/api/bills/my`, for patients only (new `BillingSecurityRules`, `hasRole("PATIENT")`):
  - `GET /api/bills/my` lists the patient's bills, newest first, each in full (lines, amounts, payment, refund). A patient has few bills, so the app needs no separate "one bill" call;
  - `GET /api/bills/my/{billId}/receipt.pdf` returns a bill's receipt.
- **Which bills:** the signed-in patient's account. A bill keeps the account's patient id, also for a family member's visit, so those are included, even after the family member is removed.
- **Cancelled bills are left out.** Those bills were never paid; the patient only left the queue or the visit was cancelled. A reopened bill shows again.
- **Someone else's bill, or a cancelled one,** gives 404 for the receipt. That way a patient can't tell another patient's bill exists.
- **Receipts** follow the desk's rule: paid, refund due or refunded; 409 for an unpaid bill.
- **What the patient's view leaves out:** who took or gave back the money (staff usernames), the patient id and the item ids.
- **Code:** a separate `MyBillDTO` and `MyBillService`, so the desk's view stays as it is.

**Frontend:**

- **Bill dialog**, opened from both places:
  - the bill number, status, who the visit was for, the doctor and the visit date;
  - the lines, subtotal, discount and total;
  - what happens next: "Pay ₹… at the front desk", "Paid ₹… by UPI on …", "₹… will be refunded at the front desk" or "Refunded ₹… by cash on …";
  - **Download receipt**.
- **My appointments:** visits with a bill get a **Bill** button. That includes today's visit once checked in, which still sits under Upcoming. The bills are loaded once with the appointments and matched to the visits by appointment id.
- **My account:** a **Bills** card under Family members:
  - newest first, the first 5 with **Show all**;
  - each row shows the number, date, doctor, who the visit was for, the total and the status;
  - each row has **View** and, where there is one, **Receipt**;
  - unpaid bills are pointed out ("₹… to pay at the front desk").
- **Shared code:** the desk's bill page and the patient dialog share the receipt download, the date-time format and the "has a receipt" rule (moved to `billFormat.js`).

## Steps

| Step | Work | Checked by |
|---|---|---|
| PB.1 | **Patient bill endpoints** (`MyBillService`, `MyBillDTO`, `MyBillController`, `BillingSecurityRules`); endpoint and access snapshots; billing README, module docs. | `BillingH2Test` (own and family member's bills, not others', not cancelled ones, receipt rules, other roles refused); mutation checks |
| PB.2 | **Bill dialog and the Bill button in My appointments**; the shared helpers moved out of the desk's bill page. | Tests; mutation checks; screenshots |
| PB.3 | **Bills in My account.** | Tests; mutation checks; screenshots |
| PB.4 | **Docs**, final check, push and PR link. | Full backend and frontend test runs; build |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| PB.0 Plan + decisions | ✅ done | 2026-10-13 | This file. |
| PB.1 Patient bill endpoints | ✅ done | 2026-10-13 | **Endpoints** (patients only, new `BillingSecurityRules`): `GET /api/bills/my` returns the patient's bills, newest first, each in full as `MyBillDTO` (the desk's view minus the staff user names, the patient id and the item ids); cancelled bills are left out. `GET /api/bills/my/{id}/receipt.pdf` follows the desk's rule through the new `BillDeskService.receiptOf`: 409 before payment, 404 for another patient's or a cancelled bill. **Code:** `MyBillService` finds the patient by the logged-in user (patients' `PtInfoService`; billing already declared that module); two new `BillRepository` finders, and the list fetches the lines in the same query (`@EntityGraph`). The single-bill endpoint in the plan was dropped: the list already carries each bill in full. **Tests:** `BillingH2Test` +2. A patient and their son each have a visit, another patient has one, and the desk checks in all three. The patient sees only their own and their son's bills, newest first, with lines, total and the UPI payment but no staff user name. The receipt works once paid and gives 409 before; another patient's receipt is 404 even after it is paid, and that patient sees only their own. When the son leaves the queue, his cancelled bill disappears and its receipt is 404. Receptionist, doctor and admin get 403; anonymous gets 401. **Mutation checks** (each makes a test fail): cancelled bills shown, anyone's receipt, any signed-in user allowed, oldest first. **Snapshots and docs:** +2 lines each in the endpoint and access snapshots (patients only). Module docs: billing now uses patients; only the billing diagram, its page and the overview changed, and the other diagrams (merely reordered) are left out. Billing README. Checker OK; full backend run green (app: 117 tests, 1 skipped). |
| PB.2 Bill dialog and My appointments | ✅ done | 2026-10-13 | **My appointments:** a visit with a bill shows "Bill ₹… · status" and a **Bill** button. That includes today's checked-in visit under Upcoming; on a phone the summary gets its own line. The bills load with the doctor's notes (`getMyBills`), and the appointments still show if either fails. **`MyBillDialog`:** the number and status; who the visit was for, the doctor and the date; the lines; subtotal and discount (only when there is a discount) and the total. Then what happens next: "Please pay ₹… at the front desk.", "Paid ₹… by UPI (ref. …) on …", "The visit was cancelled: the front desk will refund ₹…" or "Refunded ₹… by … on …". **Download receipt** appears when there is a receipt. **Shared with the desk's bill page:** `hasReceipt`, `formatDateTime` and `referenceNote` (now in `billFormat.js`) and `downloadReceipt.js`. **API client:** `getMyBills` and `getMyReceiptPdf`. **Tests:** `AppointmentDetails.test.js` +4: a paid bill with a discount, its payment and the receipt download (file name); today's unpaid bill (pay at the desk, no receipt) and a cancelled paid visit due for refund; the appointments without bills when they can't be loaded; a refused receipt. **Mutation checks** (each makes a test fail): a receipt button on every bill, bills matched by bill id, failing bills break the page, the discount never shown, no receipt once due for refund, the receipt asked by appointment id, the unpaid note missing. Screenshots: light and dark, desktop and phone. 230 frontend tests; the build compiles without warnings. |
| PB.3 Bills in My account | ✅ done | 2026-10-13 | **Bills** card under Family members: newest first, the first 5 and then **Show all N bills** / **Show fewer**. Each row shows the date and doctor, the bill number and who the visit was for, the total, the status, **View** (the same dialog) and **Receipt** when there is one. Unpaid bills are added up: "To pay at the front desk: ₹… (n bills)". When there are none it says "No bills yet"; when they can't be loaded it says so, and the rest of the page still works. **Tests:** `MyAccount.test.js` +2: the list, the amount to pay, which rows have a receipt, Show all / Show fewer, the receipt download and View; one unpaid bill and no Show all for a few; no bills; bills that can't be loaded. **Mutation checks** (each makes a test fail): every bill counted as to pay, all bills shown at once, a receipt on every row, failed bills shown as none, the receipt asked by appointment id, the bill count shown for a single bill. Screenshots: light and dark, desktop and phone. 232 frontend tests; the build compiles without warnings. |
