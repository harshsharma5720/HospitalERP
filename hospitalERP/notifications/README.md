# notifications (`hospital-notifications`)

Delivery channels: email (SMTP) and SMS (Twilio), plus the message texts. It knows how to send a message, not *why* — it has no idea what an appointment or a leave is. Callers pass plain data.

**Depends on:** common · **Used by:** identity, appointments

## Public API — `com.itmonteur.hospitalerp.notifications`

| Class | Purpose |
|---|---|
| `NotificationService` | Appointment messages to patient and doctor by SMS + email: `appointmentBooked`, `appointmentCancelled`, `appointmentCancelledByDoctorLeave`, `appointmentReminder`. Takes a `NotificationService.AppointmentInfo` record (names, contacts, date, time). Runs `@Async`, so a slow mail server never blocks a request; failures are logged, never thrown. |
| `SmsService` | Twilio SMS: `sendOtp`, `sendPasswordResetSms`, the appointment texts, `isEnabled()`, `mask(phone)` for logs. Without Twilio settings SMS is skipped with a warning. |
| `EmailService` | HTML emails (booking, cancellation, doctor notification, leave cancellation, reminder, password reset). User-supplied text is HTML-escaped. |

## Events

None published or consumed. (Appointments decides *when* to notify; see `AppointmentNotificationEvent` in the appointments module.)

## Internal — `notifications.internal`

`TwilioConfig`: initialises the Twilio client from the `twilio.*` properties.

No REST endpoints.

## Configuration

| Env variable | Meaning |
|---|---|
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP server (optional; without it emails are skipped). |
| `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_TRIAL_NUMBER` | Twilio (optional; without it SMS are skipped). |

## Tests

No unit tests of its own; `NotificationsH2Test` in `app` checks that appointment notifications are sent only after the booking transaction commits.

Module diagram: [docs/modules/module-notifications.puml](../../docs/modules/module-notifications.puml).
