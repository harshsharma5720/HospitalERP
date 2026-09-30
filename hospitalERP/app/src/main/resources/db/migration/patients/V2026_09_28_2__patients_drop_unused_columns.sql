-- patients: drop patient.role and patient_relative.role.
-- Unused since step 1.3: the role lives only in `users`.
-- Each drop checks information_schema first (MySQL has no DROP COLUMN IF EXISTS), so a database
-- that never had the column is left alone.

SET @stmt = IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'patient' AND column_name = 'role') > 0,
               'ALTER TABLE `patient` DROP COLUMN `role`', 'DO 0');
PREPARE drop_column FROM @stmt;
EXECUTE drop_column;
DEALLOCATE PREPARE drop_column;

SET @stmt = IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'patient_relative' AND column_name = 'role') > 0,
               'ALTER TABLE `patient_relative` DROP COLUMN `role`', 'DO 0');
PREPARE drop_column FROM @stmt;
EXECUTE drop_column;
DEALLOCATE PREPARE drop_column;
