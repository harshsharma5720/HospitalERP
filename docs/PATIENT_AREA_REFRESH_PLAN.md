# Patient area refresh — plan

The staff portals and the public site got a new look in the UI refresh ([UI_REFRESH_PLAN.md](UI_REFRESH_PLAN.md)). The patient's own pages and the login / register pages still have the old one: neon colours, an animated overlay after booking, 26 `alert()` pop-ups and a watermarked stock photo as background. This piece of work brings them onto the same design system and fixes the bugs found on the way.

## Decisions (2026-10-12)

| Question | Decision |
|---|---|
| Booking page | **Step by step on one page:** 1. who the appointment is for (the patient or a family member; gender and age come from the profile), 2. the doctor, 3. the day (the next 7 days) and a free time (morning / evening), 4. the reason and Confirm, with a summary beside the steps. A confirmation card at the end instead of the animated overlay. Rescheduling uses the same page with the person fixed. |
| Way of working | Each finished, tested step is committed without asking; the branch is pushed at the end with the PR link. |

## Bugs found while reading the pages

- **Register:** when the server doesn't require phone verification (`OTP_REQUIRED=false`) the Register button stays disabled, so nobody can register.
- **Relatives list:** opened directly (`/relatives`, a bookmark or a reload) it waits for a patient id that only the previous page passed along, and shows "Loading relatives..." forever.
- **Login:** a "Sign in with other" button that does nothing; the links reload the whole app.
- **Receptionist's profile:** the staff portal's "My profile" opens `/edit-profile`, a page of the public site, outside the portal.

## Design

- **Auth pages** (`AuthLayout`): a calm split screen — the hospital's panel on one side, the form card on the other (one column on a phone); shared form controls; errors on the form, toasts for confirmations.
- **Patient pages** use the public site's pieces (`PublicPage`, `PageIntro`, cards, badges, buttons, toasts, confirm dialog).
- **My appointments:** upcoming and past visits as cards (day, time, doctor, who it is for, status), Reschedule / Cancel (confirm dialog) for upcoming ones; the consultation summary, the prescription PDF and Book again for past ones.
- **My account** (`/edit-profile`): the patient's details (edit in place), family members (add, edit, remove), and links to appointments. The header's "Profile" pop-up becomes a "My account" link for patients. Receptionists get their profile inside the staff portal; doctors already have `/doctor/profile`.

## Steps

| Step | Work | Checked by |
|---|---|---|
| P.1 | **Login, register, forgot password** on an `AuthLayout`; the register bug fixed; the dead button and the login animation gone. | Tests (incl. registering without OTP); screenshots |
| P.2 | **Booking page**, step by step, for new bookings and rescheduling; confirmation card. | Tests; screenshots |
| P.3 | **My appointments.** | Tests; screenshots |
| P.4 | **My account and family members**; the relatives list works when opened directly; the header's profile pop-up replaced; the receptionist's profile inside the staff portal. | Tests; screenshots |
| P.5 | **Docs**, final check, push and PR link. | — |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| P.0 Plan + decisions | ✅ done | 2026-10-12 | This file. |
| P.1 Login, register, forgot password | ✅ done | 2026-10-12 | `AuthLayout` (the hospital's panel and the form; one column on a phone; light / dark switch; no site footer under it). **Register:** works again without phone verification (`OTP_REQUIRED=false` left the button disabled); a changed phone number needs a new code; toasts and messages on the form instead of 8 `alert()`. **Login:** the button that did nothing and the animation are gone; links instead of page reloads. **Forgot password:** same look, a toast at the end. Unused `LoginAnim.json` and the old popup's heartbeat animation removed. 11 tests (9 new), 5 mutations caught incl. the old register bug; screenshots light, dark, phone. |
| P.2 Booking page | ✅ done | 2026-10-12 | Step by step: who it's for (the patient or a family member as cards; gender and age asked only when the profile lacks them), the doctor (pre-chosen from a doctor's page, Book again or the visit being moved), the day (today + 6 as chips) and morning / evening with the free times, the reason; a summary with Confirm beside the steps (below them on a phone). A confirmation card replaces the animated overlay; a taken time shows the error and reloads the free times. Rescheduling: the person stays, Move appointment, back to My appointments. A family member is booked by their own name (the label with the relationship was sent before; the server uses the relative's record anyway). Header: the links and phone number no longer wrap for a logged-in patient. Unused `Loader` removed. 6 tests, 5 mutations caught; screenshots desktop, dark, phone and the confirmation (the first phone shot showed the day chips widening the page - fixed). |
