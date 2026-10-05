-- Audit log (docs/AUDIT_LOG_PLAN.md): who viewed or changed which patient record, and when.
-- Append-only: the app never updates or deletes rows. No foreign keys, so entries outlive deleted accounts.
-- `action` is varchar, not enum, so a new action needs no migration.
CREATE TABLE `audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `occurred_at` datetime(6) NOT NULL,
  `actor_user_id` bigint DEFAULT NULL,
  `actor_username` varchar(255) DEFAULT NULL,
  `actor_role` varchar(20) DEFAULT NULL,
  `action` varchar(40) NOT NULL,
  `patient_id` bigint DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `details` varchar(255) DEFAULT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_log_patient` (`patient_id`, `occurred_at`),
  KEY `idx_audit_log_actor` (`actor_username`, `occurred_at`),
  KEY `idx_audit_log_occurred` (`occurred_at`)
) ENGINE=InnoDB;
