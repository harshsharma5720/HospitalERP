-- Log out everywhere (docs/SAFER_LOGIN_TOKENS_PLAN.md): every access token carries the account's token version and is
-- refused once the version has moved on ("log out of all devices", a password reset, a deactivation).
-- Existing accounts start at 0, like the tokens given out before this migration.
ALTER TABLE `users`
  ADD COLUMN `token_version` int NOT NULL DEFAULT 0;
