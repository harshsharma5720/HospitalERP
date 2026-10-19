-- Login sessions (docs/SAFER_LOGIN_TOKENS_PLAN.md): the refresh tokens of each login (a session), stored as their
-- SHA-256 hash only. A token is replaced on every use; a session ends at logout, on reuse of an old token, a password
-- reset or a deactivation. Rows go with their user.

CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `session_id` varchar(36) NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `session_expires_at` datetime(6) NOT NULL,
  `replaced_at` datetime(6) DEFAULT NULL,
  `ended_at` datetime(6) DEFAULT NULL,
  `end_reason` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_tokens_hash` (`token_hash`),
  KEY `idx_refresh_tokens_user` (`user_id`),
  KEY `idx_refresh_tokens_session` (`session_id`),
  CONSTRAINT `fk_refresh_tokens_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;
