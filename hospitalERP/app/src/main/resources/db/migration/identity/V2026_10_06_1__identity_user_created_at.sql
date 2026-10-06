-- When an account was created (docs/ADMIN_DASHBOARD_PLAN.md: "new patients" on the admin dashboard).
-- Existing accounts get no date (NULL): counting new accounts starts with the ones created after this migration.
ALTER TABLE `users`
  ADD COLUMN `created_at` datetime(6) DEFAULT NULL;
