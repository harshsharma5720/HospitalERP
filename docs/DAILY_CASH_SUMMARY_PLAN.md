# Daily cash summary — plan

The front desk records every payment and refund on the bill: when, how (cash, card, UPI, or online through Razorpay) and who took it. Nothing adds them up. At the end of a day nobody can say what the drawer should hold, or what came in by card and UPI, without going through the bills one by one. This is the follow-up named in [BILLING_PLAN.md](BILLING_PLAN.md) ("a daily cash summary for the front desk") and the next item in the suggested order of [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md).

## Decisions (2026-10-19)

| Question | Decision |
|---|---|
| What | A **summary** of a day — payments and refunds per method, who took them, the bills behind them — **and closing the day:** the desk counts the cash, enters it, and sees the difference from what the app expected. The close is kept and goes into the audit log. Payments after a close count towards the next close. |
| Who sees what | **The desk** (receptionists, and admins at the front desk) sees **today**, for the whole desk, split by who took each payment, and closes the day. **Admins** see **any day or a range**, with the closes. |
| Printing | **A PDF like the receipts:** the day's totals, refunds, closes and who took what, to hand over or file. |
| Way of working | Each finished, tested step is committed without asking; the branch is pushed at the end with the PR link. |

## Design

**What counts on a day** (the hospital's clock, `Clock`):
- **A payment** is a bill paid that day (`paidAt`), whatever happened to it later (a paid visit cancelled afterwards is still money taken that day). Its amount is the bill's total; a bill can't change once paid.
- **A refund** is a bill refunded that day (`refundedAt`), always the whole bill.
- Per method (cash, card, UPI, online): the payments (number, amount), the refunds, and the net. The total net.
- **Who:** per receptionist (`paidBy`, `refundedBy`). Online payments have no receptionist: they show as "Online (the patient)". An online refund was started at the desk, so it counts under the receptionist.
- **The lines:** time, what (payment or refund), bill number, patient, doctor, method, reference, by whom, amount; in the order they happened.
- **Still unpaid:** the bills of the day's visits that nobody has paid yet (number, amount), so the desk can chase them.
- A late online payment that couldn't pay its bill is refunded by Razorpay itself and never reached the desk; it isn't in the summary.

**Closing the day:**
- A close covers the **cash** taken and given back **since the previous close**; the first ever covers its own day. A day that wasn't closed is in the next close, because its cash is still in the drawer. Closing twice a day is fine: the second covers what came in after the first (e.g. an evening shift).
- The desk sees what the drawer should hold (cash in − cash out), counts the cash (without the change float) and enters it. When the amounts differ, a note is needed ("Why is the cash different?"). The difference is kept: counted − expected, negative when cash is missing.
- Stored in a new table `cash_closes` (billing, Flyway): when, by whom, the period's start, cash in, cash out, expected, counted, difference and the note. Card, UPI and online totals stay in the summary; they are checked against the card machine's and the bank's statements, not counted.
- A close can't be changed. Two closes at the same moment: the period's start is unique, so the second gets "Someone has just closed the day" (409).
- Every close goes into the audit log (`CASH_DAY_CLOSED`, with the amounts).

**Endpoints** (billing):
- The desk: `GET /api/receptionist/cash-summary` (today, with the cash since the last close), `GET /api/receptionist/cash-summary/today.pdf`, `POST /api/receptionist/cash-closes` (`{counted, note}`).
- Admins: `GET /api/admin/cash-summary?date=` (a day), `GET /api/admin/cash-summary/range?from=&to=` (at most 31 days: a row per day, the totals and the closes), `GET /api/admin/cash-summary/pdf?date=`.

**The PDF** (OpenPDF, the receipts' letterhead and style): the day, the totals per method (payments, refunds, net), who took what, the closes, and the lines. Amounts as "Rs. 1,250.00", like the receipts.

**Frontend:**
- A new menu item **Day summary** under Bills: `/cash-summary` for receptionists, `/admin/cash-summary` for admins.
- The desk's page: today's tiles (taken, refunded, net; per method), who took what, the lines, still unpaid, and the closes made today. A card **Close the day** shows the cash expected since the last close; **Close the day…** asks for the counted amount (and the note when it differs) and shows the result. **Download PDF**.
- The admin's page: the same for any day (a date picker, today first), and a range view: a row per day (taken, refunded, net, cash, closes, the difference highlighted when not zero).

## Steps

| Step | Work | Checked by |
|---|---|---|
| CS.1 | **The day's summary (backend):** `CashSummaryService`, the DTOs, the desk's and the admin's day and range endpoints. | End-to-end tests with payments and refunds on known days (methods, people, a paid visit cancelled later, online, unpaid, other days not counted, the range limit, access); mutation checks |
| CS.2 | **Closing the day (backend):** `cash_closes` (Flyway), `CashCloseService`, the endpoint, the audit action, "since the last close" in the summary, the day's closes. | End-to-end tests (expected cash, the period after an earlier close, a day not closed, the note, two closes at once, audit, access); mutation checks |
| CS.3 | **The PDF (backend).** | Tests on the PDF's text; mutation checks |
| CS.4 | **The desk's page (frontend):** Day summary, Close the day, the PDF; the menu. | Tests; mutation checks; screenshots |
| CS.5 | **Admins (frontend):** any day, a range, the closes. | Tests; mutation checks; screenshots |
| CS.6 | **Docs** (README, billing README, the end-to-end test guide, the ideas doc, the MySQL runbook), final check, push and PR link. | Full backend and frontend runs; build |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| CS.0 Plan + decisions | ✅ done | 2026-10-19 | This file. |
| CS.1 The day's summary (backend) | ✅ done | 2026-10-19 | **`CashSummaryService`** (billing): the bills paid or refunded in a period, with their lines (one query, `BillRepository.findPaidOrRefundedBetween`), become payments and refunds; a payment counts on its day whatever happened to the bill later, a refund on its own day, both for the bill's total. `CashSummaryDTO`: the four methods always (payments, taken, refunds, refunded, net), the totals, per person by name with online payments (no one) last, the lines in time order, the day's visits still unpaid. `CashRangeDTO`: every day of the range with the cash's own net, the methods and totals. **Endpoints** (`CashSummaryController`): `GET /api/receptionist/cash-summary` (today; receptionists and admins), `GET /api/admin/cash-summary?date=` and `GET /api/admin/cash-summary/range?from=&to=` (admins; up to today, at most 31 days; 400 otherwise). **Tests:** new `CashSummaryH2Test` (2, in FeatureFlowH2Test's context; its payments moved to March 2021, which no other test uses): cash with an extra item, card at the day's first moment, UPI at the next day's, a visit paid on the day and refunded at the next day's first moment, online, unpaid; per method, per person, the lines, unpaid, the next day, three days; today for the desk (before and after a payment); access; which days. **Mutation checks:** 12, all killed by a named test. The first run let one survive (the next day's first moment counted): the query already leaves such bills out, and no bill had one move in the day and the other at midnight. The refunded visit now does, and the six checks touching it were run again. Endpoint and access snapshots: +3 lines each. Billing README. Checker OK; full backend run green (215 tests, 1 skipped). |
