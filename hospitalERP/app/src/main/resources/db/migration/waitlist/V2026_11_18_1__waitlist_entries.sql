-- The waitlist (docs/WAITLIST_PLAN.md): a patient - or one of their family members - waiting for a doctor on a full
-- day, in one shift or either (`shift` empty). Told by the app and SMS when a time that day frees up. An entry is
-- deleted with its patient, family member or doctor, and 30 days after its day.

CREATE TABLE `waitlist_entries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `patient_id` bigint NOT NULL,
  `relative_id` bigint DEFAULT NULL,
  `doctor_id` bigint NOT NULL,
  `visit_date` date NOT NULL,
  `shift` varchar(10) DEFAULT NULL,
  `status` varchar(10) NOT NULL,
  `joined_at` datetime(6) NOT NULL,
  `joined_by` varchar(100) NOT NULL,
  `told_at` datetime(6) DEFAULT NULL,
  `ended_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_waitlist_entries_doctor_date` (`doctor_id`, `visit_date`, `status`),
  KEY `idx_waitlist_entries_patient` (`patient_id`, `status`),
  KEY `idx_waitlist_entries_date` (`visit_date`),
  CONSTRAINT `fk_waitlist_entries_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_waitlist_entries_relative` FOREIGN KEY (`relative_id`) REFERENCES `patient_relative` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_waitlist_entries_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `doctor` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;
