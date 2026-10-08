-- Walk-in patients registered at the front desk (docs/WALK_IN_REGISTRATION_PLAN.md): a patient record may have
-- no login account and no email. Every new record gets a creation time; existing records stay empty.
-- (The unique key on email stays: several records without an email are allowed.)
ALTER TABLE `patient`
  MODIFY COLUMN `email` varchar(255) DEFAULT NULL,
  ADD COLUMN `created_at` datetime(6) DEFAULT NULL;
