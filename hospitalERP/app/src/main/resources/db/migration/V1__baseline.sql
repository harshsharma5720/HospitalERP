-- Baseline: the schema as it was when Flyway was introduced (plan step 4.1, 2026-09-28).
--
-- Generated from the entities by Hibernate on MySQL 8 (ddl-auto=create, then mysqldump --no-data),
-- plus the five columns that older databases still have although the entities dropped them in
-- step 1.3 (removed again by the staff/patients migrations).
--
-- Runs only on an EMPTY database. An existing database (built by the old ddl-auto=update) is
-- marked as version 1 instead (spring.flyway.baseline-on-migrate=true); this file never runs there.
-- Never edit this file: change the schema with a new migration in db/migration/<module>/.

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `appointments` (
  `age` int NOT NULL,
  `date` date NOT NULL,
  `is_completed` bit(1) NOT NULL,
  `reminder_sent` bit(1) DEFAULT NULL,
  `appointmentid` bigint NOT NULL,
  `doctor_id` bigint DEFAULT NULL,
  `patient_id` bigint DEFAULT NULL,
  `relative_id` bigint DEFAULT NULL,
  `slot_id` bigint DEFAULT NULL,
  `message` text,
  `patient_name` varchar(255) NOT NULL,
  `gender` enum('FEMALE','MALE','OTHER') NOT NULL,
  `shift` enum('EVENING','MORNING') NOT NULL,
  `status` enum('CANCELLED_BY_DOCTOR','CANCELLED_BY_PATIENT','COMPLETED','CONFIRMED','SCHEDULED') NOT NULL,
  PRIMARY KEY (`appointmentid`),
  KEY `FKgpgce3qtc5fajyl4j5srcjkcf` (`doctor_id`),
  KEY `FKcl9b1a19a01yhjcdibna1gjl` (`patient_id`),
  KEY `FKlhw4nl7086tjlidym9f5af1l6` (`relative_id`),
  KEY `FKf8qrv9g386dae81yfkj1qgs77` (`slot_id`),
  CONSTRAINT `FKcl9b1a19a01yhjcdibna1gjl` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`),
  CONSTRAINT `FKf8qrv9g386dae81yfkj1qgs77` FOREIGN KEY (`slot_id`) REFERENCES `slots` (`id`),
  CONSTRAINT `FKgpgce3qtc5fajyl4j5srcjkcf` FOREIGN KEY (`doctor_id`) REFERENCES `doctor` (`id`),
  CONSTRAINT `FKlhw4nl7086tjlidym9f5af1l6` FOREIGN KEY (`relative_id`) REFERENCES `patient_relative` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `appointments_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB;

CREATE TABLE `consultations` (
  `follow_up_date` date DEFAULT NULL,
  `pulse` int DEFAULT NULL,
  `temperature` double DEFAULT NULL,
  `weight_kg` double DEFAULT NULL,
  `appointment_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) DEFAULT NULL,
  `blood_pressure` varchar(255) DEFAULT NULL,
  `diagnosis` text NOT NULL,
  `notes` text,
  `symptoms` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKp0pg0r434dp34iesx6lj69b8m` (`appointment_id`),
  CONSTRAINT `FKp77tpwkqp4e3fxdi9d7eo44cx` FOREIGN KEY (`appointment_id`) REFERENCES `appointments` (`appointmentid`)
) ENGINE=InnoDB;

CREATE TABLE `doctor` (
  `id` bigint NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) NOT NULL,
  `profile_image` varchar(255) DEFAULT NULL,
  `user_name` varchar(255) DEFAULT NULL,
  `specialist` enum('CARDIOLOGY','DENTISTRY','DERMATOLOGY','NEUROLOGY','NOT_ASSIGNED','ORTHOPEDICS','PEDIATRICS') NOT NULL,
  `password` varchar(255) DEFAULT NULL, -- unused since step 1.3, dropped by a later migration
  `role` enum('ADMIN','DOCTOR','PATIENT','RECEPTIONIST') DEFAULT NULL, -- unused since step 1.3, dropped by a later migration
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjdtgexk368pq6d2yb3neec59d` (`email`),
  UNIQUE KEY `UK3q0j5r6i4e9k3afhypo6uljph` (`user_id`),
  CONSTRAINT `FK11wrxiolc8qa2e64s32xc2yy4` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `doctor_schedules` (
  `end_time` time(6) DEFAULT NULL,
  `slot_minutes` int NOT NULL,
  `start_time` time(6) DEFAULT NULL,
  `working` bit(1) NOT NULL,
  `doctor_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') NOT NULL,
  `shift` enum('EVENING','MORNING') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKknghkk9yboevnj21juhaomjdx` (`doctor_id`,`day_of_week`,`shift`),
  CONSTRAINT `FKh8492aw4t30m9v2cbcone3qyn` FOREIGN KEY (`doctor_id`) REFERENCES `doctor` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `doctor_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB;

CREATE TABLE `leave_request` (
  `end_date` date DEFAULT NULL,
  `start_date` date DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL,
  `status` enum('APPROVED','PENDING','REJECTED') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfw8uf3srojce00747x3qjvvmc` (`user_id`),
  CONSTRAINT `FKfw8uf3srojce00747x3qjvvmc` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `patient` (
  `dob` date DEFAULT NULL,
  `patient_aadhar_no` bigint DEFAULT NULL,
  `patient_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `contact_no` varchar(15) DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `patient_address` varchar(255) DEFAULT NULL,
  `patient_name` varchar(255) DEFAULT NULL,
  `profile_image` varchar(255) DEFAULT NULL,
  `user_name` varchar(255) DEFAULT NULL,
  `gender` enum('FEMALE','MALE','OTHER') DEFAULT NULL,
  `role` enum('ADMIN','DOCTOR','PATIENT','RECEPTIONIST') DEFAULT NULL, -- unused since step 1.3, dropped by a later migration
  PRIMARY KEY (`patient_id`),
  UNIQUE KEY `UKbawli8xm92f30ei6x9p3h8eju` (`email`),
  UNIQUE KEY `UK6i3fp8wcdxk473941mbcvdao4` (`user_id`),
  CONSTRAINT `FKie6vajiyur53rjcl5nc2pe83t` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `patient_relative` (
  `dob` date DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `patient_aadhar_no` bigint DEFAULT NULL,
  `patient_id` bigint DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `gender` enum('FEMALE','MALE','OTHER') DEFAULT NULL,
  `relationship` enum('BROTHER','DAUGHTER','FATHER','GRANDFATHER','GRANDMOTHER','HUSBAND','MOTHER','OTHER','SISTER','SON','WIFE') DEFAULT NULL,
  `role` enum('ADMIN','DOCTOR','PATIENT','RECEPTIONIST') DEFAULT NULL, -- unused since step 1.3, dropped by a later migration
  PRIMARY KEY (`id`),
  KEY `FK4l3drg7mdfygwjmqjbkcjc0ae` (`patient_id`),
  CONSTRAINT `FK4l3drg7mdfygwjmqjbkcjc0ae` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`)
) ENGINE=InnoDB;

CREATE TABLE `prescription_items` (
  `consultation_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `dosage` varchar(255) DEFAULT NULL,
  `duration` varchar(255) DEFAULT NULL,
  `frequency` varchar(255) DEFAULT NULL,
  `instructions` varchar(255) DEFAULT NULL,
  `medicine_name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfy9o0v4i1r8si8kno0dkq2fjk` (`consultation_id`),
  CONSTRAINT `FKfy9o0v4i1r8si8kno0dkq2fjk` FOREIGN KEY (`consultation_id`) REFERENCES `consultations` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `receptionist` (
  `age` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `profile_image` varchar(255) DEFAULT NULL,
  `user_name` varchar(255) DEFAULT NULL,
  `gender` enum('FEMALE','MALE','OTHER') NOT NULL,
  `role` enum('ADMIN','DOCTOR','PATIENT','RECEPTIONIST') DEFAULT NULL, -- unused since step 1.3, dropped by a later migration
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK8c3j4hva9j643r6odutw8v9mg` (`email`),
  UNIQUE KEY `UKapxjt1s0nwi3933w3ks5r4y5e` (`user_id`),
  CONSTRAINT `FKps6s4faixxtgwxk3txl9e5t0q` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `slots` (
  `available` bit(1) NOT NULL,
  `date` date DEFAULT NULL,
  `end_time` time(6) DEFAULT NULL,
  `start_time` time(6) DEFAULT NULL,
  `doctor_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `shift` enum('EVENING','MORNING') DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKj1t5nwmrvbdmsf5vihjjkrcxp` (`doctor_id`),
  CONSTRAINT `FKj1t5nwmrvbdmsf5vihjjkrcxp` FOREIGN KEY (`doctor_id`) REFERENCES `doctor` (`id`)
) ENGINE=InnoDB;

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `phone_number` varchar(15) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `role` enum('ADMIN','DOCTOR','PATIENT','RECEPTIONIST') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB;

-- Hibernate's table-based id sequences start at 1
INSERT INTO `appointments_seq` VALUES (1);
INSERT INTO `doctor_seq` VALUES (1);

SET FOREIGN_KEY_CHECKS = 1;
