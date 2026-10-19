-- The notification centre (docs/NOTIFICATION_CENTRE_PLAN.md): each user's messages in the app, read or unread.
-- Messages older than 90 days are deleted every night; a deleted account takes its messages with it.

CREATE TABLE `inbox_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `kind` varchar(30) NOT NULL,
  `title` varchar(120) NOT NULL,
  `body` varchar(500) NOT NULL,
  `link` varchar(200) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_inbox_messages_user_created` (`user_id`, `created_at`),
  KEY `idx_inbox_messages_created` (`created_at`),
  CONSTRAINT `fk_inbox_messages_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;
