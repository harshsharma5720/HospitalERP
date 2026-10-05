# MySQL database upgrade to Flyway — runbook

**Purpose:** move an existing HospitalERP MySQL database (built by the old `ddl-auto=update`) onto the Flyway migrations of branch `refactor/modules`, safely, before that branch is merged into `main`.
**Status (2026-09-30):** not done yet. The real database lives on the **other laptop**. Everything in the code is ready and tested on a throw-away MySQL 8 (`DatabaseMigrationMySqlTest`); what's missing is a run against the real data.
**Background:** [MULTI_MODULE_PLAN.md](MULTI_MODULE_PLAN.md) step 4.1 · README section "Database migrations (Flyway)".

---

## 1. What will happen to the database

On the first start of the new code:

1. Flyway sees tables but no `flyway_schema_history` table → it **marks the database as version 1** ("baseline"). It does **not** run `V1__baseline.sql` there.
2. It runs the newer migrations:
   - two that **permanently drop 5 unused columns**: `doctor.password`, `doctor.role`, `receptionist.role` (`staff/V2026_09_28_1__staff_drop_unused_columns.sql`) and `patient.role`, `patient_relative.role` (`patients/V2026_09_28_2__patients_drop_unused_columns.sql`). Login data lives in `users`; nothing reads these columns any more.
   - one that **adds** `users.active` (existing accounts: active) and `users.deactivated_at` (`identity/V2026_09_30_1__identity_account_status.sql`, from the account-deactivation feature — only once that branch is merged).
   - one that **creates** the empty `audit_log` table (`audit/V2026_10_05_1__audit_log.sql`, from the audit-log feature — only once that branch is merged).
   All other data stays.
3. Hibernate then **validates** (`ddl-auto=validate`): every entity's table and column must exist with a compatible type. If not, the app stops with `Schema-validation: …`.

⚠ Step 3 happens **after** steps 1–2. If validation fails, the columns are already dropped. That's why this runbook takes a backup first and **rehearses on a copy** before touching the real database.

After this, Hibernate never changes the schema again; schema changes are new files in `hospitalERP/app/src/main/resources/db/migration/<module>/`.

---

## 2. Where to do it — pick one

| | Option A: on the other laptop (recommended) | Option B: copy the database to this laptop |
|---|---|---|
| When | You can work on the laptop that has the database | You want to rehearse here first |
| How | Sections 3–9 on that laptop | [Appendix A](#appendix-a--option-b-copy-the-database-to-this-laptop), then sections 5–7 here |
| The real upgrade | Happens on that laptop (section 7) | Still has to happen on the other laptop afterwards (section 7 there) |

**Privacy:** the database holds real patient data (names, contacts, medical notes). If you copy it (option B or the rehearsal copy), keep the dump file on your own machines only, don't commit it or send it by chat/email, and delete the copies when you're done (section 9).

---

## 3. Prepare (on the laptop with the database)

Checklist — run in PowerShell:

```powershell
java -version        # 17 or newer
git --version
node --version       # 18+ (for the frontend smoke test)
mysql --version      # MySQL 8.0+
```

If `mysql` / `mysqldump` aren't found, use the full path, e.g.
`& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"` (same folder for `mysqldump.exe`).

Get the code:

```powershell
cd <path>\HospitalERP
git fetch origin
git status            # must be clean; commit or stash local work first
```

Check `hospitalERP\.env` exists and note its `DB_URL` (e.g. `jdbc:mysql://localhost:3306/hospital_erp`), `DB_USERNAME`, `DB_PASSWORD`. Below, the database is called **`hospital_erp`** — replace it if yours differs.

---

## 4. Back up (never skip)

```powershell
mysqldump -u root -p --single-transaction --routines --triggers --no-tablespaces `
  --result-file="C:\db-backups\hospital_erp_before_flyway.sql" hospital_erp
```

- Use `--result-file`, **not** `> file` — PowerShell's `>` writes UTF-16 and breaks the dump.
- Create `C:\db-backups` first (`mkdir C:\db-backups`).
- Check it worked: the file is not tiny and its last line is `-- Dump completed on …`:
  ```powershell
  Get-Content C:\db-backups\hospital_erp_before_flyway.sql -Tail 1
  ```

---

## 5. Catch-up run and pre-check

### 5.1 Catch-up run on `main` (brings an older database up to the version-1 shape)

If the database was last used with code older than 2026-09-24, it lacks tables the new code needs (e.g. `consultations`, `prescription_items`, `doctor_schedules`) — and Flyway won't create them on an existing database. Starting `main` once lets the old `ddl-auto=update` add whatever is missing. It's what the app has always done on start, so it's safe; harmless if nothing is missing.

```powershell
git checkout main
git pull
cd hospitalERP
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--REMINDERS_ENABLED=false"
```

Wait for `Started HospitalErpApplication`, then stop it with `Ctrl+C`.

### 5.2 Record the data (to compare afterwards)

In `mysql -u root -p hospital_erp`:

```sql
SELECT VERSION();
SHOW TABLES;   -- expect these 13: appointments, appointments_seq, consultations, doctor, doctor_schedules,
               -- doctor_seq, leave_request, patient, patient_relative, prescription_items, receptionist, slots, users

SELECT 'users' t, COUNT(*) n FROM users UNION ALL SELECT 'patient', COUNT(*) FROM patient
UNION ALL SELECT 'patient_relative', COUNT(*) FROM patient_relative UNION ALL SELECT 'doctor', COUNT(*) FROM doctor
UNION ALL SELECT 'receptionist', COUNT(*) FROM receptionist UNION ALL SELECT 'appointments', COUNT(*) FROM appointments
UNION ALL SELECT 'slots', COUNT(*) FROM slots UNION ALL SELECT 'doctor_schedules', COUNT(*) FROM doctor_schedules
UNION ALL SELECT 'leave_request', COUNT(*) FROM leave_request UNION ALL SELECT 'consultations', COUNT(*) FROM consultations
UNION ALL SELECT 'prescription_items', COUNT(*) FROM prescription_items;
```

Write the counts down (or screenshot them).

### 5.3 Compare with the target schema (read-only check of the real database)

Build a **reference** database from the migration files, then list every difference.

```powershell
git checkout refactor/modules
git pull
mysql -u root -p -e "CREATE DATABASE hospital_erp_reference"
mysql -u root -p hospital_erp_reference -e "source hospitalERP/app/src/main/resources/db/migration/V1__baseline.sql"
mysql -u root -p hospital_erp_reference -e "source hospitalERP/app/src/main/resources/db/migration/staff/V2026_09_28_1__staff_drop_unused_columns.sql"
mysql -u root -p hospital_erp_reference -e "source hospitalERP/app/src/main/resources/db/migration/patients/V2026_09_28_2__patients_drop_unused_columns.sql"
mysql -u root -p hospital_erp_reference -e "source hospitalERP/app/src/main/resources/db/migration/identity/V2026_09_30_1__identity_account_status.sql"
mysql -u root -p hospital_erp_reference -e "source hospitalERP/app/src/main/resources/db/migration/audit/V2026_10_05_1__audit_log.sql"
```

(Run every `.sql` file that exists under `db/migration/` in version order — the last two lines only if those files exist in your checkout.)

(Run these from the repository root; use forward slashes in the `source` paths.)

Then, in `mysql -u root -p`:

```sql
SELECT 'only in hospital_erp' AS difference, r.table_name, r.column_name, r.column_type AS detail
FROM information_schema.columns r
WHERE r.table_schema = 'hospital_erp'
  AND NOT EXISTS (SELECT 1 FROM information_schema.columns f
                  WHERE f.table_schema = 'hospital_erp_reference'
                    AND f.table_name = r.table_name AND f.column_name = r.column_name)
UNION ALL
SELECT 'missing in hospital_erp', f.table_name, f.column_name, f.column_type
FROM information_schema.columns f
WHERE f.table_schema = 'hospital_erp_reference'
  AND NOT EXISTS (SELECT 1 FROM information_schema.columns r
                  WHERE r.table_schema = 'hospital_erp'
                    AND r.table_name = f.table_name AND r.column_name = f.column_name)
UNION ALL
SELECT 'different type', r.table_name, r.column_name, CONCAT(r.column_type, '  <-- now | target -->  ', f.column_type)
FROM information_schema.columns r
JOIN information_schema.columns f
  ON f.table_schema = 'hospital_erp_reference' AND f.table_name = r.table_name AND f.column_name = r.column_name
WHERE r.table_schema = 'hospital_erp' AND r.column_type <> f.column_type
ORDER BY 1, 2, 3;
```

**How to read the result:**

| Result | Meaning | Action |
|---|---|---|
| Exactly 5 rows "only in hospital_erp": `doctor.password`, `doctor.role`, `patient.role`, `patient_relative.role`, `receptionist.role` | Perfect — these are the columns the migrations drop | Continue |
| Other "only in hospital_erp" rows | Leftover columns from older versions (e.g. `receptionist.password`) | Harmless for the app (validation ignores extra columns). Note them; we can drop them later with a migration |
| "missing in hospital_erp": `users.active`, `users.deactivated_at` and the 10 columns of `audit_log` | Expected — migrations add them during the upgrade | Continue |
| Any other "missing in hospital_erp" row | The database is older than the code | Do 5.1 again (catch-up run). If it stays, **stop** and send the list |
| Any "different type" row (e.g. `varchar(255)` now, `enum(...)` target) | Column created by an older Hibernate version | **Stop.** Send the rows; the fix is a small migration (`ALTER TABLE … MODIFY COLUMN …`) added before the upgrade |

---

## 6. Rehearsal on a copy (strongly recommended)

Run the real upgrade on a copy of the real data first.

```powershell
mysql -u root -p -e "CREATE DATABASE hospital_erp_copy"
mysql -u root -p hospital_erp_copy -e "source C:/db-backups/hospital_erp_before_flyway.sql"
```

(If you did 5.1 after taking the backup, take a fresh backup first, or the copy won't include the catch-up changes.)

Start the new code **against the copy**, with day-before reminders off (so no SMS/emails go to real patients):

```powershell
git checkout refactor/modules
cd hospitalERP
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--DB_URL=jdbc:mysql://localhost:3306/hospital_erp_copy --REMINDERS_ENABLED=false"
```

Command-line arguments override `.env`, so `.env` stays pointed at the real database.

**Expected log lines, in this order:**

```
Successfully validated 5 migrations
Successfully baselined schema with version: 1
Migrating schema `hospital_erp_copy` to version "2026.09.28.1 - staff drop unused columns"
Migrating schema `hospital_erp_copy` to version "2026.09.28.2 - patients drop unused columns"
Migrating schema `hospital_erp_copy` to version "2026.09.30.1 - identity account status"
Migrating schema `hospital_erp_copy` to version "2026.10.05.1 - audit log"
Successfully applied 4 migrations to schema `hospital_erp_copy`, now at version v2026.10.05.1
Started HospitalErpApplication in … seconds
```

(Each of these features not yet in your checkout means one migration less. Without the audit log: 4 validated, 3 applied, no "audit log" line, now at v2026.09.30.1. Without account deactivation too: 3 validated, 2 applied.)

**Check the copy** (`mysql -u root -p hospital_erp_copy`):

```sql
SELECT installed_rank, version, description, type, success FROM flyway_schema_history ORDER BY installed_rank;
-- expect: 1 BASELINE, 2026.09.28.1 SQL, 2026.09.28.2 SQL, 2026.09.30.1 SQL, 2026.10.05.1 SQL, all success = 1

SELECT table_name, column_name FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'doctor' AND column_name IN ('password', 'role'))
    OR (table_name IN ('patient', 'patient_relative', 'receptionist') AND column_name = 'role'));
-- expect: empty
```

Then run the row-count query from 5.2 — the numbers must be the same.

**Smoke test** (second terminal):

```powershell
cd hospital-frontend
npm install
npm start
```

At http://localhost:3000 check, with a **test patient you create yourself** (your own email/phone — booking sends a confirmation):
- [ ] log in as admin; the user list and doctor list load
- [ ] log in as a doctor; dashboard, appointments, schedule and leave pages load
- [ ] log in as the test patient; book an appointment, see it in "Appointment details"
- [ ] as the doctor, open the appointment → Start Consultation → save with one medicine
- [ ] as the patient, download the prescription PDF
- [ ] an existing (real) patient's profile opens (read-only look is enough)

Stop both (`Ctrl+C`). If anything failed → section 8. Don't continue to section 7.

---

## 7. The real upgrade

Only after section 6 passed.

1. Take a **fresh backup** (section 4, new file name, e.g. `hospital_erp_before_flyway_final.sql`) — the rehearsal was on a copy, but data may have changed since.
2. Start the new code on the real database (`.env` already points to it):
   ```powershell
   git checkout refactor/modules
   cd hospitalERP
   .\mvnw.cmd spring-boot:run
   ```
3. Check the same log lines as in section 6 (with `hospital_erp`), the same two SQL checks, and the row counts.
4. Quick smoke test: log in as admin, doctor and patient; open an appointment and a prescription.

From now on, **don't run the old `main` code against this database** until the branch is merged: old `main` still has `ddl-auto=update` and the old entity fields, so it would add the dropped columns back.

---

## 8. If something goes wrong

| Message (start of the log / error) | Cause | What to do |
|---|---|---|
| `Schema-validation: missing table [x]` or `missing column [c] in table [t]` | Database older than the code | Restore (section 9 "Roll back"), do the catch-up run (5.1), try again. If it stays, send the message |
| `Schema-validation: wrong column type encountered in column [c] in table [t]; found [...], but expecting [...]` | Column created by an older Hibernate version | Restore; send the message — a migration `ALTER TABLE t MODIFY COLUMN c <expected>` fixes it |
| `Found non-empty schema(s) … but no schema history table` | Old code or settings (`baseline-on-migrate` missing) | Make sure you're on `refactor/modules` (`git branch`) |
| `Validate failed: Migrations have failed validation` / `checksum mismatch` | A migration file was edited after it ran | Restore the file from git (`git checkout -- <file>`); changes go in a **new** migration |
| `Detected applied migration not resolved locally` | Older code started on an upgraded database | Switch to `refactor/modules` (or after the merge: `main`) |
| `Communications link failure` | MySQL not running / wrong host or port | Start the MySQL service; check `DB_URL` |
| `Access denied for user` | Wrong `DB_USERNAME` / `DB_PASSWORD` | Fix `.env` |
| `Unknown database` | Database name in `DB_URL` wrong | Fix `DB_URL` |

Any failure during the **rehearsal**: just drop the copy (`DROP DATABASE hospital_erp_copy;`) — the real database is untouched.

---

## 9. Roll back / clean up

**Roll back the real database** (only if section 7 failed):

```powershell
mysql -u root -p -e "DROP DATABASE hospital_erp; CREATE DATABASE hospital_erp"
mysql -u root -p hospital_erp -e "source C:/db-backups/hospital_erp_before_flyway_final.sql"
git checkout main
```

The app on `main` then works exactly as before.

**After a successful upgrade, clean up:**

```sql
DROP DATABASE hospital_erp_copy;
DROP DATABASE hospital_erp_reference;
```

Keep the backup files for a few weeks, then delete them (they contain patient data).

---

## 10. After success: merge

1. On GitHub: **Pull requests → New** → base `main`, compare `refactor/modules` → create.
2. Merge with **"Create a merge commit"** (not squash), so the step-by-step history stays.
3. On every laptop: `git checkout main`, `git pull`. The upgraded database needs nothing more — Flyway sees it's at the latest version.
4. Any other developer database gets the same treatment (sections 4–7), or starts empty (Flyway then builds it from `V1__baseline.sql`).
5. Update the progress log in [MULTI_MODULE_PLAN.md](MULTI_MODULE_PLAN.md) (step 4.1: "your MySQL still to upgrade" → done).

---

## Appendix A — Option B: copy the database to this laptop

Use this to rehearse here. The real upgrade still happens on the other laptop (section 7 there).

**On the other laptop:** take the backup (section 4) — ideally after the catch-up run (5.1). Move the `.sql` file to this laptop by USB stick or your own private cloud folder (see the privacy note in section 2).

**On this laptop** (MySQL isn't installed here, Docker is — this was tested with the project's migration test):

```powershell
# 1. A MySQL 8 container on port 3307 (3306 may be taken)
docker run -d --name hospitalerp-mysql -e MYSQL_ROOT_PASSWORD=choose-a-password -p 127.0.0.1:3307:3306 mysql:8.0

# 2. Wait ~30 s until it's ready (repeat until it prints 1)
docker exec hospitalerp-mysql mysql -uroot -pchoose-a-password -e "SELECT 1"

# 3. Load the dump
docker cp C:\db-backups\hospital_erp_before_flyway.sql hospitalerp-mysql:/tmp/backup.sql
docker exec hospitalerp-mysql mysql -uroot -pchoose-a-password -e "CREATE DATABASE hospital_erp_copy"
docker exec hospitalerp-mysql sh -c "mysql -uroot -pchoose-a-password hospital_erp_copy < /tmp/backup.sql"
```

Then follow **section 6** here, with this start command:

```powershell
cd hospitalERP
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--DB_URL=jdbc:mysql://localhost:3307/hospital_erp_copy --DB_USERNAME=root --DB_PASSWORD=choose-a-password --REMINDERS_ENABLED=false"
```

For the SQL checks use `docker exec -it hospitalerp-mysql mysql -uroot -pchoose-a-password hospital_erp_copy`.
Section 5.3 works here too: create `hospital_erp_reference` in the container and `docker cp` the migration files in, then `source` them from `/tmp/…`.

**Clean up afterwards:**

```powershell
docker rm -f hospitalerp-mysql
Remove-Item C:\db-backups\hospital_erp_before_flyway.sql   # patient data
```

---

## Appendix B — Reference facts

| Item | Value |
|---|---|
| Migration folder | `hospitalERP/app/src/main/resources/db/migration/` |
| Migrations | `V1__baseline.sql` (full schema, only for empty databases), `staff/V2026_09_28_1__staff_drop_unused_columns.sql`, `patients/V2026_09_28_2__patients_drop_unused_columns.sql`, `identity/V2026_09_30_1__identity_account_status.sql` (adds `users.active`, `users.deactivated_at`), `audit/V2026_10_05_1__audit_log.sql` (creates the empty `audit_log` table) |
| Settings (`app/src/main/resources/application.properties`) | `spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.baseline-on-migrate=true`, `spring.flyway.baseline-version=1` |
| Tables (13, plus `audit_log` with the audit-log feature) | appointments, appointments_seq, consultations, doctor, doctor_schedules, doctor_seq, leave_request, patient, patient_relative, prescription_items, receptionist, slots, users (+ `flyway_schema_history` after the upgrade) |
| Columns dropped | doctor.password, doctor.role, patient.role, patient_relative.role, receptionist.role |
| `.env` keys used here | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REMINDERS_ENABLED` |
| Automated proof | `DatabaseMigrationMySqlTest` (needs Docker): upgrade of a pre-Flyway database with data + fresh install give the identical schema |
| Last tested | 2026-09-28 on MySQL 8.0 (Docker), Flyway 11.7.2, Spring Boot 3.5.5 |
