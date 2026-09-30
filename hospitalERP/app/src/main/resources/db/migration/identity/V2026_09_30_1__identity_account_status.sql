-- identity: accounts are deactivated instead of deleted (docs/ACCOUNT_DEACTIVATION_PLAN.md).
-- A deactivated user can't log in and is hidden from directories; all history is kept.
-- Existing users stay active.

ALTER TABLE `users`
  ADD COLUMN `active` bit(1) NOT NULL DEFAULT b'1',
  ADD COLUMN `deactivated_at` datetime(6) DEFAULT NULL;
