-- Outbox of appointment emails and SMS (docs/RELIABLE_NOTIFICATIONS_PLAN.md): one row per message, written in
-- the same transaction as the booking or cancellation, delivered and retried by a background sender.
-- Delivered and skipped rows are deleted after 30 days, failed ones after 90 days.
CREATE TABLE `notification_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel` varchar(10) NOT NULL,
  `recipient` varchar(255) NOT NULL,
  `subject` varchar(255) DEFAULT NULL,
  `body` text NOT NULL,
  `description` varchar(100) NOT NULL,
  `status` varchar(10) NOT NULL,
  `attempts` int NOT NULL,
  `next_attempt_at` datetime(6) NOT NULL,
  `give_up_at` datetime(6) NOT NULL,
  `last_error` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `sent_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notification_outbox_due` (`status`, `next_attempt_at`),
  KEY `idx_notification_outbox_created` (`created_at`)
) ENGINE=InnoDB;
