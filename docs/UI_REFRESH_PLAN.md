# UI refresh: the staff portals — plan

**Branch:** `feature/ui-refresh` (from `main` = 6c44a8b)
**Why:** the frontend looks dated and uneven. There is no shared design — every page hard-codes its own colours (e.g. a neon cyan text colour in dark mode) and the Tailwind theme defines none; admins, doctors and receptionists each have a different layout (a dark left sidebar, a right sidebar with a profile card, and the public website's navigation bar); 56 browser `alert()` pop-ups report errors and confirmations; the admin sidebar has buttons that lead nowhere.

## Decisions (2026-10-12)

| Question | Decision |
|---|---|
| What first | **The UI refresh**, before more features — later features are then built with the new design. |
| Where to start | **The staff portals** (admin, doctor, front desk): one app layout, shared components, toasts instead of `alert()`. The public site and the patient area follow in a later piece of work. |
| Look | **Clean clinical:** white and soft-grey surfaces, one confident blue accent, the Inter font, generous spacing, subtle shadows; a calm dark mode (no neon). |
| Way of working | Each finished, verified step is committed without asking; the branch is pushed at the end with the PR link. |
| Home page (added 2026-10-12) | The public **home page** joins this piece of work as its own step: a professional landing page, better imagery, a cleaner welcome popup (no animation over the buttons) and scroll animations. |

## Design

- **Design tokens** (`src/index.css`, `tailwind.config.js`): colours as CSS variables, light and dark — `canvas` (page), `surface` (cards), `surface-2` (table heads, subtle fills), `line` (borders), `ink` / `ink-muted` (text), `primary` (blue), and the status colours `success`, `warning`, `danger`, `info`. Each tone has a solid fill for buttons (`bg-danger`, white text, the same in both modes), a darker hover fill (`bg-danger-hover`) and an ink for text, icons and light tints (`text-danger-ink`, `bg-danger-ink/10`), lighter in dark mode. All text pairs read at 4.5:1 or better in both modes. Pages use the token classes (`bg-surface`, `text-ink-muted`, …) instead of hex codes, so dark mode follows automatically. The Inter font (Google Fonts).
- **Shared components** (`src/shared/ui/`): `Button`, `Card`, `PageHeader`, form fields (`Field`, `Input`, `Select`, `Textarea`), `Badge`, `Notice` (a message on the page), the table parts (`Table`, `Th`, `Td`, `TableMessage`), `EmptyState`, `Pagination`, and **toasts** (`useToast`) and a **confirm dialog** (`useConfirm`) to replace `alert()` / `confirm()`.
- **One staff layout** (`StaffLayout`): a left sidebar with the menu of the logged-in role — front desk: Appointments, Walk-in, Patients, Bills, Queue screen; doctor: Dashboard, Today's queue, Appointments, Schedule, Leave, Profile; admin: Dashboard, the front-desk pages, Users, Doctors, Leave approvals, Fees, Audit log, Notifications — and a top bar with the user (name, role; the doctor's photo), the light / dark switch and Log out. On a phone the sidebar becomes a drawer. Replaces the admin sidebar, the doctor's right sidebar and the website navigation on the front-desk pages; the dead admin buttons go. The menus live in `src/shared/layout/staffMenus.js`; the light / dark choice is applied at start-up (`src/shared/theme.js`), so the portals get it too. On the public site, staff get one **Staff portal** link back (instead of the old per-role links, one of which - the doctor's - led nowhere).
- **Pages** are restyled with the components, keeping their behaviour, texts and tests.

## Steps

| Step | Work | Checked by |
|---|---|---|
| U.1 | **Design foundation:** tokens, Inter, base styles, the shared components, toasts and the confirm dialog. | Component tests; build; a screenshot sheet of the components, light and dark |
| U.2 | **Staff layout:** `StaffLayout` with the role menus and the top bar; admin, doctor and front-desk pages moved into it. | Layout tests (menu per role, active page, log out, phone drawer); existing tests; screenshots |
| U.3 | **Front-desk pages:** Appointments, Walk-in, Patients, patient page, Bills, bill page. | Existing tests; screenshots |
| U.4 | **Doctor portal:** Dashboard, Today's queue, Appointments and the consultation form, Schedule, Leave, Profile; `alert()` → toasts / confirm. | Tests; screenshots |
| U.5 | **Admin portal:** Dashboard, Users, Register user, Doctors, Leave approvals, a doctor's appointments, Audit log, Notifications, Fees; `alert()` → toasts / confirm. | Tests; screenshots |
| U.6 | **Home page:** the public landing page rebuilt in the clean clinical look — hero, services, doctors, how booking works, contact — with better imagery, a cleaner welcome popup and scroll-reveal animations (off for people who ask for reduced motion). | Tests; screenshots light / dark and phone |
| U.7 | **Docs**, final check, push and PR link. | — |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| U.0 Plan + decisions | ✅ done | 2026-10-12 | This file. |
| U.1 Design foundation | ✅ done | 2026-10-12 | Tokens (fill / hover / ink per tone, contrast checked in both modes), Inter, the shared components in `src/shared/ui/` with `ToastProvider` and `ConfirmProvider` around the app; 23 component tests, each of 5 mutations caught; screenshot sheet checked light and dark (first sheet showed white on light fills in dark mode, fixed). |
| U.2 Staff layout | ✅ done | 2026-10-12 | `StaffLayout` + `staffMenus.js`; front-desk routes under one layout route, `AdminLayout` / `DoctorLayout` wrap their routes in it; `AdminSidebar`, `DoctorRightSidebar`, `SitePage` and the navbar's portal variant removed; the doctor portal no longer waits for the profile to show its pages. 17 layout tests (menus per role, active page incl. sub-pages, profile name / photo / fallback, log out, theme, phone drawer), each of 8 mutations caught; screenshots desktop light / dark and a 390 px phone with the drawer. |
| U.3 Front-desk pages | ✅ done | 2026-10-12 | Appointments, Walk-in, Patients, patient page, Bills and bill page rebuilt from the shared components, texts and behaviour unchanged (all their tests pass as they were). Visit and bill states give a badge tone instead of colour classes (new tests: `appointmentFormat`, `billFormat`; 3 mutations caught). `Table plain` for tables inside a card. Bill page: items and payment side by side on wide screens. Screenshots of every page, light and dark. |
| U.4 Doctor portal | ✅ done | 2026-10-12 | Dashboard (numbers, a completion bar instead of two charts of the same two numbers, quick links), Today's queue, Appointments (a table; confirm dialog and toasts instead of `confirm()` / `alert()`), the consultation form on the new shared `Modal`, Schedule, Leave (the made-up "Remaining: 12" and tab-dependent counts removed; the request form in a modal) and Profile (Cancel added). `downloadPrescription` reports errors to a toast in the portals. New tests for the modal, appointments, leave, dashboard and profile (153 frontend tests); 7 mutations caught; screenshots of every page. |
| U.5 Admin portal | ✅ done | 2026-10-12 | Dashboard (tokens for the chart's chrome; the series colours kept after re-validating every touching pair on the new dark surface #111827), Users (search and role filter; the dead Edit button removed), Add user (labelled form, back to Users when done), Doctors (ranking, today's working / on-leave filter, on-leave badge), Leave approvals, a doctor's appointments, Audit log, Notifications, Fees. Confirm dialog and toasts replace every `confirm()` / `alert()` in the staff portals. New `danger-ghost` button. New tests for Add user, Doctors and Leave approvals (166 frontend tests); 7 mutations caught; screenshots incl. the chart drawn at a fixed size, light and dark. |
