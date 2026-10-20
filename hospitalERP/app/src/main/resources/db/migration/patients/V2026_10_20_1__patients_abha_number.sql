-- The ABHA number (Ayushman Bharat Health Account, docs/ABHA_NUMBER_PLAN.md) of patients and family members: the 14
-- digits only, optional, and one number per person - unique in each table (the app also checks across both).
-- Existing records get none.

ALTER TABLE `patient`
  ADD COLUMN `abha_number` varchar(14) DEFAULT NULL,
  ADD UNIQUE KEY `uk_patient_abha_number` (`abha_number`);

ALTER TABLE `patient_relative`
  ADD COLUMN `abha_number` varchar(14) DEFAULT NULL,
  ADD UNIQUE KEY `uk_patient_relative_abha_number` (`abha_number`);
