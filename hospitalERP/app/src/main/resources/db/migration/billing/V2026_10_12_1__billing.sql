-- Billing at the front desk (docs/BILLING_PLAN.md): a consultation fee per doctor, one bill per appointment (made at
-- check-in) with its items, and the payment / refund recorded on the bill. No foreign keys to appointments, patients
-- or doctors: bills are financial records and keep the names they were made with (like the audit log).

CREATE TABLE `doctor_fees` (
  `doctor_id` bigint NOT NULL,
  `consultation_fee` decimal(10,2) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`doctor_id`)
) ENGINE=InnoDB;

CREATE TABLE `bills` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bill_number` varchar(20) DEFAULT NULL,
  `appointment_id` bigint NOT NULL,
  `patient_id` bigint DEFAULT NULL,
  `doctor_id` bigint DEFAULT NULL,
  `patient_name` varchar(255) NOT NULL,
  `doctor_name` varchar(255) DEFAULT NULL,
  `visit_date` date NOT NULL,
  `status` varchar(12) NOT NULL,
  `discount` decimal(10,2) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `paid_at` datetime(6) DEFAULT NULL,
  `payment_method` varchar(10) DEFAULT NULL,
  `payment_reference` varchar(100) DEFAULT NULL,
  `paid_by` varchar(100) DEFAULT NULL,
  `refunded_at` datetime(6) DEFAULT NULL,
  `refund_method` varchar(10) DEFAULT NULL,
  `refund_reference` varchar(100) DEFAULT NULL,
  `refunded_by` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bills_appointment` (`appointment_id`),
  UNIQUE KEY `uk_bills_number` (`bill_number`),
  KEY `idx_bills_status_date` (`status`, `visit_date`),
  KEY `idx_bills_visit_date` (`visit_date`)
) ENGINE=InnoDB;

CREATE TABLE `bill_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bill_id` bigint NOT NULL,
  `description` varchar(200) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `consultation` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_bill_items_bill` (`bill_id`),
  CONSTRAINT `fk_bill_items_bill` FOREIGN KEY (`bill_id`) REFERENCES `bills` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;
