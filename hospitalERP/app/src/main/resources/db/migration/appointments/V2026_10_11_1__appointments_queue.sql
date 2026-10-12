-- Today's queue (docs/TODAYS_QUEUE_PLAN.md): when the patient checked in at the front desk, their token for the
-- day (kept from check-in on), and when the doctor called them in. Empty for every existing appointment.
ALTER TABLE `appointments`
  ADD COLUMN `checked_in_at` datetime(6) DEFAULT NULL,
  ADD COLUMN `token_number` int DEFAULT NULL,
  ADD COLUMN `called_at` datetime(6) DEFAULT NULL;
