# End-to-end test guide

Every feature built so far, and how to check it by hand in the running app: where it is, who uses it, what to set up, the steps, and what you should see. It is kept up to date after each feature (last update: 2026-10-19, safer logins).

Tick the **Tested** column in the overview as you go. Each section links to the feature's plan for the details.

## Before you start

**Run the app:** with Docker (`docker compose up -d`, see the README "Run with Docker") or locally (backend `./mvnw spring-boot:run -pl app`, frontend `npm start`). Use a test database, not the hospital's real one.

**Accounts you need:**
- **Admin:** created at start-up from `ADMIN_USERNAME` / `ADMIN_PASSWORD` in `.env`.
- **A doctor and a receptionist:** Admin → **Users** → *Register user*, with the role.
- **Their profiles:** the doctor fills in a name and speciality under **Profile**; the receptionist under **My profile**.
- **A patient:** **Register** on the site. Without Twilio set `OTP_REQUIRED=false`, or nobody can sign up.
- **A second browser or a private window:** to be patient and staff at the same time.

**Settings that change what you can test (`.env`):**

| Setting | For |
|---|---|
| `MAIL_*`, `TWILIO_*` | Real emails / SMS. Without them messages are *skipped*, which you can still see under Admin → Notifications. |
| `HOSPITAL_NAME`, `HOSPITAL_ADDRESS`, `HOSPITAL_PHONE` | The letterhead on prescriptions and receipts; the phone number in the cancellation message. |
| `BILLING_DEFAULT_FEE` | The fee for doctors without their own. |
| `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, `RAZORPAY_WEBHOOK_SECRET` | Online payment (test keys `rzp_test_…`); off while empty. |
| `APPOINTMENT_CHANGE_CUTOFF_HOURS` | The cancellation cut-off (default 2). Set it to e.g. 48 to test the cut-off without waiting for a visit 2 hours away. |
| `REMINDERS_CRON` | The reminder job (hourly). Set it to `0 * * * * *` (every minute) to see a reminder quickly. |
| `JWT_EXPIRATION_MS` | The access token's lifetime (default 15 minutes). Set it to `60000` to see the app renew it every minute. |
| `AUTH_STAFF_HOURS`, `AUTH_PATIENT_IDLE_DAYS`, `AUTH_PATIENT_MAX_DAYS` | How long a login lasts (staff 12 hours; patients 7 days after the last use, at most 90). Set `AUTH_STAFF_HOURS=1` to see a staff login end within an hour. |

**A suggested order:** a hospital day, start to finish.
1. Accounts.
2. Doctor's schedule and fee.
3. A patient books.
4. Walk-in.
5. Check-in and queue.
6. Consultation.
7. Bill and payment (at the desk, then online).
8. Patient's bills.
9. Cancellations and refunds.
10. Dashboards and analytics.
11. Admin tools.

## Overview

| # | Feature | Who | Where | Tested |
|---|---|---|---|---|
| 1 | Register, log in, forgot password | Everyone | `/register`, `/login`, `/forgot-password` | ☐ |
| 2 | Public site, light / dark, phone layout | Everyone | `/`, `/treatments`, `/doctors`, `/doctor-profile/:id`, `/contact`, `/about` | ☐ |
| 3 | Staff portals: menus per role, profiles | Staff | Admin `/admin/…`, doctor `/doctor/…`, front desk | ☐ |
| 4 | Doctor's schedule | Doctor | `/doctor/schedule` | ☐ |
| 5 | Booking, rescheduling and cancelling | Patient | `/appointments`, `/appointment-details` | ☐ |
| 6 | Cancellation cut-off | Patient, front desk | `/appointment-details` | ☐ |
| 7 | My account and family members | Patient | `/edit-profile`, `/add-relative` | ☐ |
| 8 | Appointment emails / SMS, reminders, resend | Admin (automatic) | `/admin/notifications` | ☐ |
| 9 | Walk-in registration and booking | Front desk | `/walk-in` | ☐ |
| 10 | Patient search, a patient's page | Front desk | `/patients` | ☐ |
| 11 | Front-desk appointment list | Front desk | `/receptionist-appointments` | ☐ |
| 12 | Today's queue: check-in, doctor's queue, waiting-room screen | Front desk, doctor, everyone | Queue column, `/doctor/queue`, `/queue-screen` | ☐ |
| 13 | Consultation notes and prescriptions | Doctor, patient | Doctor's queue / appointments, My appointments | ☐ |
| 14 | Leave and leave approval | Doctor, admin | `/doctor/leave-management`, `/admin/leave-approval` | ☐ |
| 15 | Consultation fees | Admin | `/admin/fees` | ☐ |
| 16 | Bills at the front desk: items, discount, payment, receipt, refund | Front desk | `/bills`, **Bill** in the appointment list | ☐ |
| 17 | Patients' bills and receipts | Patient | My appointments → **Bill**, My account → **Bills** | ☐ |
| 18 | Online payment (Razorpay), webhook, refund online | Patient, front desk | Bill window, My account, the desk's bill page | ☐ |
| 19 | Admin dashboard | Admin | `/admin/dashboard` | ☐ |
| 20 | Doctor analytics | Doctor, admin | `/doctor/dashboard`, Doctors → **Analytics** | ☐ |
| 21 | Account deactivation, reactivation, permanent delete | Admin | `/admin/manage-users`, `/admin/manage-doctor` | ☐ |
| 22 | Audit log | Admin | `/admin/audit-log` | ☐ |
| 23 | Automatic backups | Operator | `backups/` (Docker) or the Windows task | ☐ |
| 24 | Safer logins: staying logged in, log out of all devices | Everyone | My account / Profile → **Logged-in devices**; admins: top bar | ☐ |

---

## 1. Register, log in, forgot password
Plan: [PATIENT_AREA_REFRESH_PLAN.md](PATIENT_AREA_REFRESH_PLAN.md), section 10 of the [ideas doc](CODE_REVIEW_AND_IDEAS.md).
1. **Register** a patient (with `OTP_REQUIRED=true` and Twilio, enter the code sent by SMS). Expect: you land logged in; the header shows **My appointments** and **My account**.
2. Log out; log in with a wrong password, then the right one. Expect: a clear error, then success. After 5 wrong passwords the username is locked for 15 minutes ("Too many failed login attempts").
3. **Forgot password?** → username → code by SMS / email → new password. Expect: the same message whether or not the account exists; you can log in with the new password. A new password also logs the account out everywhere (section 24).
4. Log in as admin, doctor and receptionist. Expect: each lands in their own portal.

## 2. Public site
Plan: [UI_REFRESH_PLAN.md](UI_REFRESH_PLAN.md).
1. Open the home page logged out. Expect: hero, real figures and doctors, specialities, the visit step by step; sections fade in as you scroll; a welcome dialog once per visit.
2. **Treatments**, **Doctors** (speciality filter and name search; the filter stays in the address), a doctor's profile, **Contact Us**, **About Us**. Expect: only real details, with the hospital's phone, address and hours.
3. Switch dark mode (moon icon); narrow the window to phone width. Expect: everything readable, no sideways scrolling, a menu button on phones.

## 3. Staff portals
Plan: [UI_REFRESH_PLAN.md](UI_REFRESH_PLAN.md).
1. Log in as each staff role. Expect: the role's menu on the left (a drawer on a phone), the current page marked, your name, the dark switch and Log out.
2. Receptionist → **My profile**; doctor → **Profile**. Change a detail and save. Expect: a confirmation; it is still there after a reload.

## 4. Doctor's schedule
Plan: section 10 of the [ideas doc](CODE_REVIEW_AND_IDEAS.md).
1. Doctor → **Schedule**: set morning and evening hours and the slot length for a few days; try **Copy Monday to Tue–Fri**; save.
2. As a patient, book that doctor. Expect: only those hours offered, in slots of that length; days off offer nothing. Booked times are never removed by a later schedule change.

## 5. Booking, rescheduling and cancelling
Plan: [PATIENT_AREA_REFRESH_PLAN.md](PATIENT_AREA_REFRESH_PLAN.md).
1. Patient → **Book an appointment**: who it's for (you or a family member), the doctor, a day and a free time, the reason → **Confirm**. Expect: a confirmation card; the visit under **My appointments → Upcoming**.
2. **Reschedule** it to another time. Expect: the new time; the old one is free again.
3. **Cancel** another visit. Expect: a confirm dialog; then the visit is under **Past** as "Cancelled".
4. A past visit that was never completed shows as **Missed**; a completed one has the doctor's notes, **Prescription** and **Book again**.

## 6. Cancellation cut-off
Plan: [CANCELLATION_CUTOFF_PLAN.md](CANCELLATION_CUTOFF_PLAN.md). Tip: set `APPOINTMENT_CHANGE_CUTOFF_HOURS=48` for this test, so tomorrow's visit is already "too close".
1. With the default (2 h): an upcoming visit says "You can cancel or move it until … ".
2. Book a visit that starts within the cut-off (or use the tip). Expect: no Cancel / Reschedule. Instead: "Less than … hours to go: to cancel or move it, please call …", and the phone number calls on a phone.
3. Leave My appointments open past a visit's deadline, then press Cancel. Expect: the server refuses with the same message.
4. Front desk: cancel that same visit from the appointment list. Expect: it works; the desk is never limited.

## 7. My account and family members
Plan: [PATIENT_AREA_REFRESH_PLAN.md](PATIENT_AREA_REFRESH_PLAN.md).
1. **My account**: **Edit** your details; the Aadhaar number shows masked (XXXX XXXX 1234); save. Expect: saved; a refused change keeps the form open with the reason.
2. **Family members**: **Add** one (name, relationship, gender, date of birth), **Edit**, **Remove** (confirm dialog). Expect: they appear as "who it's for" when booking; their past visits stay after removal.
3. The side card shows your next visit.

## 8. Appointment emails / SMS, reminders, resend
Plan: [RELIABLE_NOTIFICATIONS_PLAN.md](RELIABLE_NOTIFICATIONS_PLAN.md).
1. Book and cancel a visit. Expect: emails / SMS to the patient and doctor. Without mail / Twilio set up they show as *skipped* under Admin → **Notifications**.
2. Stop the mail server (or use a wrong `MAIL_PASSWORD`) and book. Expect: the message *failed* after its retries; **Resend** once the server works. The dashboard tile "Undelivered notifications" counts it.
3. Reminders: a visit tomorrow gets an SMS and email once (with `REMINDERS_CRON` every minute you see it within a minute); after a reschedule it is reminded again.

## 9. Walk-in registration and booking
Plan: [WALK_IN_REGISTRATION_PLAN.md](WALK_IN_REGISTRATION_PLAN.md).
1. Front desk → **Walk-in**: type a phone number. Expect: the patients with that number (a family can share one; "+91 98765 43210" = "9876543210").
2. Register a new patient (name, phone, gender), pick a doctor; the earliest free time is preselected → **Book**. Expect: booked; an SMS to that phone.
3. Book a walk-in for **today**. Expect: checked in straight away, with a token.
4. Two desks booking the same time: the second gets "taken" and the times still free; no extra patient record is left behind.

## 10. Patient search, a patient's page
Plan: [PATIENT_SEARCH_PLAN.md](PATIENT_SEARCH_PLAN.md).
1. **Patients**: search by part of a name, a phone number, and `#41` (a patient ID); empty lists everyone, 20 per page.
2. Open a patient. Expect: details (Aadhaar masked), upcoming and all appointments ("Missed" for past ones never completed), **Book an appointment** → Walk-in with that patient.
3. Go back. Expect: the same search and page (kept in the address).

## 11. Front-desk appointment list
Plan: [PATIENT_SEARCH_PLAN.md](PATIENT_SEARCH_PLAN.md).
1. **Appointments**: today by default; filter by dates, doctor, status (upcoming / completed / cancelled) and patient; 20 per page; each patient links to their page.

## 12. Today's queue
Plan: [TODAYS_QUEUE_PLAN.md](TODAYS_QUEUE_PLAN.md). Needs visits **today**.
1. Front desk → appointment list, **Queue** column: **Check in** today's patients in a different order from their booked times. Expect: tokens follow the booked order, not the arrival order; **Undo** works until the doctor calls them.
2. Doctor → **Today's queue**: with you, waiting (by time, with tokens), not arrived, done. **Call next**, **Call** a particular patient, **Not here** (back to waiting).
3. Open `/queue-screen` on another screen (no login). Expect: per doctor, the token being served and the next ones, without names; a changed token is highlighted. All three pages refresh by themselves every few seconds.
4. Rescheduling a checked-in visit clears its check-in.

## 13. Consultation notes and prescriptions
Plan: section 10 of the [ideas doc](CODE_REVIEW_AND_IDEAS.md).
1. Doctor: from the queue (or appointments) **Start consultation**. Fill in vitals, symptoms, diagnosis, medicines, advice and a follow-up, with the patient's previous visits shown → save. Expect: the visit is completed.
2. Patient → My appointments → the past visit. Expect: diagnosis, the doctor's notes, **Prescription** downloads a PDF with the hospital's letterhead.
3. Another patient, or a receptionist, can't open it.

## 14. Leave and leave approval
1. Doctor → **Leave**: ask for days off. Admin → **Leave approvals**: approve (or reject).
2. Expect: the doctor's visits in those days are cancelled, and patients notified. A visit already paid becomes **Refund due** on its bill (see 16). The doctor shows as "On leave today" on the Doctors page.

## 15. Consultation fees
Plan: [BILLING_PLAN.md](BILLING_PLAN.md).
1. Admin → **Fees**: set a doctor's fee (0 to 1,00,000, at most 2 decimals). Expect: wrong amounts refused; doctors without a fee use `BILLING_DEFAULT_FEE`.

## 16. Bills at the front desk
Plan: [BILLING_PLAN.md](BILLING_PLAN.md).
1. Check a patient in (12). Expect: a bill (number `B-2026-…`) with "Consultation — Dr. …" at the doctor's fee; **Bill** in the appointment list opens it.
2. Add an item (e.g. Dressing ₹150), remove it, give a discount. The consultation line can't be removed.
3. **Record payment** by Cash / Card / UPI (with a reference). Expect: "Paid … by …, received by <you>"; nothing can change any more; **Download receipt** gives a PDF.
4. **Bills**: today by default; filter by date, status, patient or bill number.
5. Undo a check-in. Expect: the unpaid bill is *cancelled*, and reopens at the next check-in. Cancel a paid visit. Expect: **Refund due**; **Record refund**; the receipt shows the refund.

## 17. Patients' bills and receipts
Plan: [PATIENT_BILLS_PLAN.md](PATIENT_BILLS_PLAN.md).
1. Patient → My appointments: a visit with a bill shows "Bill ₹… · status" and **Bill**. The window shows the items, discount, total and what happens next.
2. My account → **Bills**: newest first, the first 5 then **Show all**, **View**, **Receipt**, and "To pay at the front desk: ₹…".
3. Family members' visits are included; another patient's bills never are; bills cancelled before payment are left out.

## 18. Online payment (Razorpay)
Plan: [ONLINE_PAYMENT_PLAN.md](ONLINE_PAYMENT_PLAN.md); set-up in the README, "Online payment (Razorpay)". Needs Razorpay **test** keys.
1. Without keys: unpaid bills only say "Please pay … at the front desk".
2. With keys: an unpaid bill says "Pay … online now, or at the front desk"; **Pay ₹… online** (bill window, or **Pay online** in My account) opens Razorpay Checkout.
3. Pay with a Razorpay test card or test UPI id. Expect: "Paid ₹… online. Thank you!"; the bill turns **Paid** ("by online payment, ref. pay_…"); **Download receipt** works. The desk sees "Paid … by online payment", with no "received by".
4. Close Checkout without paying. Expect: nothing changes.
5. Start paying, then (as the desk) take cash for the same bill, then finish paying online. Expect: the patient is told why, and that the payment is being refunded. At the top of the desk's bill page: "An online payment of ₹… couldn't be applied: this bill was already paid. It was refunded automatically (refund rfnd_…)". Same when the desk adds an item meanwhile, or cancels the visit.
6. Cancel a visit paid online. Expect: **Refund due**; the desk's **Refund ₹… online**; the bill turns **Refunded** ("by online payment, ref. rfnd_…"). The money shows as refunded in the Razorpay dashboard.
7. *Optional:* the webhook needs a public address (a tunnel such as ngrok), set up in the Razorpay dashboard with `RAZORPAY_WEBHOOK_SECRET`. Pay, then close the browser before it confirms. Expect: the bill still turns **Paid** within a moment.

## 19. Admin dashboard
Plan: [ADMIN_DASHBOARD_PLAN.md](ADMIN_DASHBOARD_PLAN.md).
1. Admin → **Dashboard**: today's figures (appointments, doctors on leave, active accounts, undelivered notifications) and **7 / 30 / 90 days**. The period shows appointments per day (stacked: completed, missed, upcoming, cancelled), cancellation and missed rates, new patients, and the busiest specialities and doctors.
2. Hover a column. Expect: a tooltip. **Show the numbers** shows a table. Check a figure against what you did in this test.

## 20. Doctor analytics
Plan: [DOCTOR_ANALYTICS_PLAN.md](DOCTOR_ANALYTICS_PLAN.md). Best after a few days of visits; waiting times need patients checked in and called through the queue; earnings need paid bills.
1. Doctor → **Dashboard**: quick links, then the last **30 days** (switch to 7 / 90). Tiles: visits, no-shows (missed ÷ past visits not cancelled), cancelled, average wait (check-in → called in), earned.
2. Charts:
   - **Visits per day**, stacked like the admin dashboard;
   - **Busy hours** and **Busy days**;
   - **Waiting time** per day (days without measured waits have no bar);
   - **Earnings**: earned, refunds and still unpaid, then per day.
   
   Expect: each chart has a tooltip and **Show the numbers**.
3. Compare with what happened: the visits you completed, cancelled or let be missed; the waits you created in the queue; the bills you took.
4. Admin → **Doctors** → **Analytics** for that doctor. Expect: the same figures, under the doctor's name, with "← Doctors".
5. A doctor sees only their own. The receptionist and patients have no way in.
6. On a phone: the date labels thin out and nothing runs off the screen.

## 21. Account deactivation
Plan: [ACCOUNT_DEACTIVATION_PLAN.md](ACCOUNT_DEACTIVATION_PLAN.md).
1. Admin → **Users**: **Deactivate** a patient with future visits. Expect: those visits cancelled and notified; the patient can't log in ("This account has been deactivated…"); the history is kept. **Reactivate**: they can log in again; cancelled visits stay cancelled.
2. Doctors page: **Deactivate** a doctor. Expect: "Deactivated" badge; they disappear from the public doctor list and booking.
3. **Delete permanently** an account with appointments. Expect: refused ("… Deactivate it instead."). For an account without any it works. You can't deactivate or delete yourself.

## 22. Audit log
Plan: [AUDIT_LOG_PLAN.md](AUDIT_LOG_PLAN.md).
1. Do something audited: a doctor opens a consultation, a patient downloads a prescription, someone edits a patient profile, the desk searches patients, an admin deactivates an account.
2. Admin → **Audit log**: filter by patient, user, action and days. Expect: each action with time, user and role, patient, record, details (the names of changed fields, never their values) and IP; newest first; 50 per page.

## 23. Automatic backups
Plan: [MYSQL_BACKUPS_PLAN.md](MYSQL_BACKUPS_PLAN.md); how to use and restore: [BACKUPS.md](BACKUPS.md), which has its own checklist.
1. Docker: the `backup` service writes to `backups/` every night at 02:00. Run one by hand as BACKUPS.md says, and check `LAST_RUN.txt` says OK.
2. Windows: install the task (`tools/backup/install-backup-task.ps1`), run it once, and check the backup folder.
3. **Practice a restore** on a test database (BACKUPS.md).

## 24. Safer logins
Plan: [SAFER_LOGIN_TOKENS_PLAN.md](SAFER_LOGIN_TOKENS_PLAN.md). You need two browsers (or a normal and a private window) and the browser's developer tools (F12).
1. Log in as a patient and **reload** the page; open the site in a second tab. Expect: still logged in in both (a short "Loading..." first).
2. Developer tools → **Application**. Expect: no `jwtToken` under Local Storage; under Cookies a `hospital_refresh` cookie, **HttpOnly**, path `/api/auth`, expiring in 7 days (for a staff login: 12 hours).
3. Keep using the app for more than 15 minutes (or set `JWT_EXPIRATION_MS=60000` and wait a minute). Expect: no interruption; under **Network** a `refresh` call shortly before each access token runs out.
4. **Log out** in one tab. Expect: the other tab is logged out too.
5. Log the same patient in on both browsers. In one: **My account** → **Logged-in devices** → **Log out of all devices**. Expect:
   - a question first ("Log out of all devices?"); **Cancel** changes nothing;
   - after **Log out everywhere**: this browser shows the login page with "You're logged out of all devices.";
   - in the other browser, the next click (e.g. **My appointments**) leads to the login page: "Your session ended. Please log in again."
6. Staff: the doctor's **Profile** and the receptionist's **My profile** have the same card. Admins have **Log out everywhere** in the top bar (an icon on a phone). Try it as one of them.
7. **Password reset:** logged in on browser A, use **Forgot password?** for the same account on browser B. Expect: browser A's next action leads to "Your session ended…".
8. **Deactivation:** an admin deactivates a patient who is logged in, then reactivates them. Expect: in the patient's open browser the next action leads to the login page, also after the reactivation; logging in again works.
9. **Staff limit (optional):** with `AUTH_STAFF_HOURS=1`, log in as the receptionist. Expect: an hour later, however busy, "Your session ended…".
10. **After the upgrade:** a browser that was logged in with the old version is logged out once (its old token is dropped); logging in works as usual.
