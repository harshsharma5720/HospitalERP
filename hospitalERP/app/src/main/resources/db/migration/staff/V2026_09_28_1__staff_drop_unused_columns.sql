-- staff: drop doctor.password, doctor.role and receptionist.role.
-- Unused since step 1.3: the login data (password, role) lives only in `users`.
-- Each drop checks information_schema first (MySQL has no DROP COLUMN IF EXISTS), so a database
-- that never had the column is left alone.

SET @stmt = IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'doctor' AND column_name = 'password') > 0,
               'ALTER TABLE `doctor` DROP COLUMN `password`', 'DO 0');
PREPARE drop_column FROM @stmt;
EXECUTE drop_column;
DEALLOCATE PREPARE drop_column;

SET @stmt = IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'doctor' AND column_name = 'role') > 0,
               'ALTER TABLE `doctor` DROP COLUMN `role`', 'DO 0');
PREPARE drop_column FROM @stmt;
EXECUTE drop_column;
DEALLOCATE PREPARE drop_column;

SET @stmt = IF((SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'receptionist' AND column_name = 'role') > 0,
               'ALTER TABLE `receptionist` DROP COLUMN `role`', 'DO 0');
PREPARE drop_column FROM @stmt;
EXECUTE drop_column;
DEALLOCATE PREPARE drop_column;
