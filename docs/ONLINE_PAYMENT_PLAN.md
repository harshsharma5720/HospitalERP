# Online payment (Razorpay) — plan

Patients see their bills in the app ([PATIENT_BILLS_PLAN.md](PATIENT_BILLS_PLAN.md)) but can only pay at the front desk. This piece of work lets them pay an unpaid bill online with Razorpay (UPI, cards, net banking, wallets) from the same place, so they don't have to queue at the desk. It finishes idea #18 in [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md).

## Decisions (2026-10-15)

| Question | Decision |
|---|---|
| When patients can pay online | **After check-in, from the bill.** Bills are still made at check-in. An unpaid bill gets "Pay ₹… online" in the bill window and in My account. Paying when booking (prepaying) is left out. |
| Refunds of online payments | **The desk clicks "Refund ₹… online"** on a cancelled visit's bill paid online. The money goes back through Razorpay and the refund is recorded. Nothing is refunded automatically when a visit is cancelled. |
| Razorpay account | **Not yet.** Automated tests use a fake Razorpay. Online payment stays switched off until the keys are set in `.env`; with test keys (`rzp_test_…`) it can be tried without real money. |
| Way of working | Each finished, tested step is committed without asking; the branch is pushed at the end with the PR link. |

## How a payment works

1. The patient taps **Pay ₹… online**. The server checks the bill:
   - it is theirs;
   - it is unpaid;
   - it is at least ₹1.

   It then creates a Razorpay **order** for the bill's total in paise (receipt = the bill number) and keeps it in a new table `online_payments`. An unused order for the same amount is reused; if the bill changed, a new order is made.
2. The app opens **Razorpay Checkout** (`checkout.js`) with that order. The patient pays there; the card or UPI details never reach our server.
3. Checkout gives the app the payment id and a signature, and the app sends them to the server. The server checks the signature (HMAC-SHA256 of `order id|payment id` with the key secret, using the order id it stored), then fetches the payment from Razorpay:
   - it must belong to the order;
   - the amount must match;
   - an *authorized* payment is captured, so we don't depend on the dashboard's auto-capture setting.

   The bill becomes **paid**: method `ONLINE`, the Razorpay payment id as the reference, the time. Who received it is left empty; the desk sees "Paid online".
4. **Webhook as a safety net:** if the patient's phone loses the connection after paying, Razorpay's `payment.captured` webhook reaches `/api/payments/razorpay/webhook`. The server checks its `X-Razorpay-Signature` (HMAC-SHA256 of the raw body with the webhook secret) and applies the payment the same way. Steps 3 and 4 can arrive in either order or both. Applying is idempotent and locks the bill.

**When a payment can't be applied**, it is refunded right away through Razorpay and recorded on its `online_payments` row. That happens when:
- the desk took cash meanwhile;
- the desk changed the bill after the order was made;
- the visit was cancelled.

The patient is told why. If that refund fails, the desk's bill page shows the payment, so they can refund it in the Razorpay dashboard.

**Refund at the desk:** a bill paid online that becomes *refund due* gets **Refund ₹… online**. The server asks Razorpay to refund the payment (normal speed, 5–7 working days) and records it: method `ONLINE`, the refund id, who and when. A refund in cash still works as before.

## Design

- **Configuration:**
  - `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET`: online payment is on only when both are set.
  - `RAZORPAY_WEBHOOK_SECRET`: without it the webhook refuses everything, and the in-app confirmation still works.
  - `RAZORPAY_API_URL`: defaults to `https://api.razorpay.com/v1`; the tests point it at a fake.
  - All four go in `application.properties`, `docker-compose.yml`, `.env.docker.example` and the README's `.env` example.
- **Billing module code:**
  - `RazorpayClient`: Spring `RestClient`, basic auth with the key; create order, fetch payment, capture, refund. No Razorpay SDK.
  - `RazorpaySignatures`.
  - `OnlinePayment` entity and repository.
  - `OnlinePaymentService`.
  - `PaymentMethod.ONLINE`: the desk can't choose it for a cash payment or refund.
  - The receipt says "online payment (Razorpay)".
- **Database:** Flyway `billing/V2026_10_15_1__online_payments.sql` adds `online_payments`:
  - bill id;
  - Razorpay order id, unique;
  - amount;
  - status: created / paid / refunded / refund failed;
  - payment id and refund id;
  - why it was refunded;
  - times.
- **Endpoints:**
  - Patients:
    - `MyBillDTO` gains `payOnline`: online payment is on, the bill is unpaid and at least ₹1;
    - `POST /api/bills/my/{id}/online-payment` returns the order for Checkout (key id, order id, amount, currency, bill number, the patient's name, email and phone);
    - `POST /api/bills/my/{id}/online-payment/confirm` takes `{orderId, paymentId, signature}` and returns the bill, or 409 with the reason.
  - Razorpay: `POST /api/payments/razorpay/webhook`, open to everyone and checked by signature.
  - Front desk:
    - `POST /api/receptionist/bills/{id}/refund-online`;
    - `BillDTO` lists online payments that couldn't be applied and whose refund failed.
- **Frontend:**
  - `features/billing/razorpayCheckout.js` loads `checkout.js` once and opens it. A `useOnlinePayment` hook runs the whole flow:
    - order;
    - Checkout;
    - confirm;
    - toast;
    - the bill updated in place.
  - **Pay ₹… online** goes in the bill window and in the My account rows. Closing Checkout does nothing, and a failed payment shows Razorpay's reason.
  - The desk's bill page shows "Paid ₹… online (ref. pay_…)", **Refund ₹… online**, and any online payment that needs a manual refund.

## Steps

| Step | Work | Checked by |
|---|---|---|
| OP.1 | **Patients pay online (backend):** configuration, `RazorpayClient`, signatures, `online_payments`, order and confirm endpoints, `payOnline`, `ONLINE` method, receipt label; payments that can't be applied are refunded. | Unit tests (signatures; the client against a fake HTTP server); end-to-end tests with a fake Razorpay (paid, captured, failed, changed bill, cash meanwhile, refund failing, other patients); mutation checks |
| OP.2 | **Webhook and refund online (backend);** the desk sees online payments whose refund failed. | End-to-end tests (webhook signature, duplicates, either order with the app; cancelled visit; refund online); mutation checks |
| OP.3 | **Pay online in the app (patients).** | Tests with a fake Checkout; mutation checks; screenshots |
| OP.4 | **Front desk:** online payments on the bill page, Refund online, payments needing a manual refund. | Tests; mutation checks; screenshots |
| OP.5 | **Docs and configuration files**, how to try it with test keys, final check, push and PR link. | Full backend and frontend runs; build |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| OP.0 Plan + decisions | ✅ done | 2026-10-15 | This file. Razorpay's API checked against its docs: orders in paise with a receipt of at most 40 characters; the payment signature; fetch, capture and refund a payment; webhook signature and `x-razorpay-event-id`. |
| OP.1 Patients pay online (backend) | ✅ done | 2026-10-15 | **Configuration:** `app.razorpay.*` from `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET`, `RAZORPAY_API_URL`. **`RazorpayClient`:** Spring `RestClient` with basic auth and 5 s / 20 s timeouts; create order, fetch / capture / refund a payment; a Razorpay error or no answer becomes `RazorpayException`. **`RazorpaySignatures`:** HMAC-SHA256 hex, compared in constant time. **Database:** `online_payments` (Flyway `billing/V2026_10_15_1__online_payments.sql`) with `OnlinePayment`, `OnlinePaymentStatus` (created, paid, refunding, refunded, refund failed) and a repository that locks the row by order id. **`OnlinePaymentService`:** `start` makes or reuses the order and fills in the patient's details. `confirm` checks the signature against the stored order id, then applies the payment. Applying fetches the payment, captures it if it is only authorized, then in one transaction locks the attempt and the bill. A payment handled before gets the same outcome again. A payment that can't be applied is marked refunding, refunded at Razorpay outside the transaction, then marked refunded or refund failed. The patient gets a 409 saying why. When Razorpay can't be reached the answer is 502 with a plain message. **Bills:** `PaymentMethod.ONLINE`; the desk can't record it. The desk's changes, payments and refunds lock the bill (`BillRepository.findByIdForUpdate`), so a cash payment and an online one can't overwrite each other. `MyBillDTO.payOnline`. The receipt says "online payment (Razorpay)". Two patient endpoints under `/api/bills/my/{id}/online-payment`. `TestApi` now has `bookedForToday` and `freeSlotsTomorrow`, shared with `BillingH2Test`. **Tests:** `RazorpaySignaturesTest` (2, against a value computed outside Java). `RazorpayClientTest` (5) against a local HTTP server: request shapes, basic auth, unknown fields, errors, keys off, signatures. New `OnlinePaymentH2Test` (4, own context and database, against `FakeRazorpay` on a local port): the order is in paise and reused; a wrong signature is refused; paying, confirming twice and the desk's view work; an authorized payment is captured; a failed one leaves the bill unpaid, then a retry pays it. Payments are refunded when the bill changed, was paid in cash meanwhile, or the refund failed (marked, not retried). Another patient is refused; the desk can't record `ONLINE`; a ₹0 bill can't be paid online; Razorpay unreachable gives 502. `BillingH2Test`: no online payment without keys. **Mutation checks** (each makes a named test fail): signature not checked, orders never reused, a changed bill still paid, an authorized payment not captured, not idempotent, the desk may record ONLINE, ₹0 payable, a failed refund not marked, the desk shown as receiver. **Found on the way:** the backend mutation script called `bash` from Python, which on this machine is WSL's bash, where `mvnw` finds no Java. Every run failed before testing, so the first OP.1 run, and PB.1's checks on 2026-10-13, reported "killed" without proof. The script now uses Git Bash by full path, a baseline run must pass first, and only a named failing test counts. Re-run that way, PB.1's four checks and OP.1's nine all make a named test fail. Endpoint and access snapshots: +2 lines each (patients only). `DatabaseMigrationMySqlTest`, which ran this time because Docker was up, lists the new migration; the table migrates on MySQL 8, Hibernate validates the entity, and the table starts empty. Checker OK; full backend run green (190 tests, 1 skipped). |
| OP.2 Webhook and refund online (backend) | ✅ done | 2026-10-15 | **Webhook:** `RazorpayWebhookController` `POST /api/payments/razorpay/webhook`, open to everyone (`BillingSecurityRules.endpointRules`). `OnlinePaymentService.webhook` checks `X-Razorpay-Signature` over the raw bytes; wrong or missing gives 400. `payment.captured` and `order.paid` for one of our orders are applied the same way as the app's confirmation; other events and orders are ignored (200). When Razorpay can't be reached it answers 503, so Razorpay sends the event again. No table of event ids is kept, because applying is repeatable anyway. **Refund online:** `POST /api/receptionist/bills/{id}/refund-online` (`refundOnline`) refunds a refund-due bill paid online at Razorpay outside a transaction. It records the refund on the locked bill: `ONLINE`, the refund id, who and when. A bill paid at the desk or not due gives 409, Razorpay refusing gives 502, and no keys gives 409 with the dashboard route. **Desk view:** `BillDTO.unappliedPayments`, filled by `BillDeskService` in every answer: refunding, refunded or refund failed, with the reason and the refund id. The reasons now read well for the desk too ("The bill changed after the payment was started"). **Tests:** `OnlinePaymentH2Test` +2, plus the desk-view checks. Webhook: no signature or the wrong secret is refused; the payment is applied, sent again, applied again after the app's confirmation, and the app confirms afterwards; `payment.failed` and other orders are ignored; Razorpay down gives 503, then the payment is applied. Refund online: refused before the cancellation and for patients; done once, with its refund at Razorpay; the patient sees it; refused for a bill paid in cash. A payment landing after the visit was cancelled is refunded, and the desk sees it. **Mutation checks** (each makes a named test fail): webhook signature not checked, the webhook never applies, no retry when Razorpay is down, the webhook needs a login, a desk-paid bill refunded online, the online refund not recorded, refunded payments not shown to the desk. Endpoint and access snapshots: +2 lines each (the webhook open to everyone, the refund for the front desk). Checker OK; full backend run green (192 tests, 1 skipped). |
| OP.3 Pay online in the app (patients) | ✅ done | 2026-10-15 | **`razorpayCheckout.js`** loads Razorpay's `checkout.js` once; a failed load can be tried again. It opens Checkout with the server's order and the patient's details, and resolves with what the server checks (our order id, the payment id, the signature), or null when Checkout is closed. **`useOnlinePayment`** runs order → Checkout → confirm. It then shows a toast and updates the bill in place. On 409 (the payment was refunded) the bills are reloaded, and the reason stays shown with the bill: a refund message mustn't vanish like a toast. **Bill window:** a bill with `payOnline` says "Pay ₹… online now, or at the front desk." and has **Pay ₹… online**. A refund due on an online payment goes "to the account you paid from". Without `payOnline` nothing changes. **My account:** **Pay online** on those rows opens the bill and starts paying there, and the notice reads "Still to pay: ₹… (n bills). Pay online here, or at the front desk." `methodLabel("ONLINE")` is "online payment". API: `startOnlinePayment`, `confirmOnlinePayment`. **Tests:** `AppointmentDetails.test.js` +3: paid online end to end with what Checkout and the server get; Checkout closed, then a refunded payment explained and the bills reloaded; no button without `payOnline`. `MyAccount.test.js` +1: pay from the list, after a 502 first. New `razorpayCheckout.test.js` (2). **Mutation checks** (fixed script, baseline first; each makes a named test fail): the button without `payOnline`, closing Checkout counted as paying, bills not reloaded, the open bill not updated, the error not shown with the bill, the script added every time, a failed script never retried, Pay online on every unpaid row, the notice ignoring online payment. Screenshots: the bill window light and dark, phone, the refund message, My account. 241 frontend tests; the build compiles without warnings. |
| OP.4 Front desk | ✅ done | 2026-10-15 | **Bill page:** a payment made online reads "Paid ₹… by online payment (ref. pay_…) on …", with no "received by". A cancelled visit paid online gets **Refund ₹… online**, saying the money goes back to the account the patient paid from (5-7 working days), with the desk refund form below as "Or record a refund given at the desk". **Online payments that came too late** (`unappliedPayments`) are listed: refunded automatically (with the refund id), or a danger notice to refund by hand in the Razorpay dashboard. API: `refundBillOnline`. **Tests:** `BillPage.test.js` +3: refund online and its record; a refused online refund; the payments that came too late. A card payment offers no online refund. **Mutation checks** (each makes a named test fail): refund online offered for card payments, a failed automatic refund not flagged, unapplied payments hidden. Screenshots light and dark. |
| OP.5 Docs, configuration and final check | ✅ done | 2026-10-15 | **Configuration:** `docker-compose.yml` passes the three `RAZORPAY_*` settings to the backend (`docker compose config` checked); they are also in `.env.docker.example` and the README's `.env` example. **README:** the patient row; a features row; the migrations table; a new section "Online payment (Razorpay)" (trying it with test keys, the webhook, going live); the environment table. **Other docs:** the MySQL runbook has 11 migrations and 19 tables. [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md) has the feature status, idea #18 done, the suggested order, and a section "Online payment". The follow-ups in the billing and patient-bills plans are marked done. **Final check:** 192 backend tests (1 skipped; MySQL 8 included), checker OK; 241 frontend tests, and the build compiles without warnings. |

## Follow-ups

- **A real test payment** with Razorpay test keys, and the webhook through a tunnel: needs a Razorpay account (steps in the README).
- Paying when booking (prepaying), if wanted later: bills would have to be made at booking.
