# Backups — how they work and how to restore

Every night the database and the uploaded profile images are backed up automatically. This guide shows how to check that it works, how to set it up on Windows, and how to get the data back. Design and decisions: [MYSQL_BACKUPS_PLAN.md](MYSQL_BACKUPS_PLAN.md).

## In short

| | Docker (`docker compose up`) | Windows with a normal MySQL installation |
|---|---|---|
| Runs | automatically — the `backup` service, every night at 02:00 | a Windows scheduled task, every night at 02:00 (later if the laptop was off) — **set it up once**, see below |
| Backups in | `backups\` next to `docker-compose.yml` (`BACKUP_DIR` in `.env`) | `C:\db-backups\hospital-erp` |
| Did last night work? | `backups\LAST_RUN.txt` | `C:\db-backups\hospital-erp\LAST_RUN.txt` (and `backup.log`) |
| Restore | `restore` in the backup container | `tools\backup\restore-windows.ps1` |

**Kept:** the 7 newest backups, plus the newest one of each of the last 4 weeks (about 11). Older ones are deleted after each good backup — never after a failed one.

**One backup = one folder** named after its time, e.g. `2026-10-08_020000`:

| File | What |
|---|---|
| `hospital_erp.sql.gz` | the whole database (mysqldump, compressed) |
| `uploads.tar.gz` | the uploaded profile images |
| `SHA256SUMS` | checksums — the restore refuses a backup whose files changed |
| `manifest.txt` | when, from where, file sizes |

A backup only appears under its final name when it is complete and checked (the archives can be read, the dump ends with MySQL's "Dump completed" line). **`LAST_RUN.txt`** says `OK` or `FAILED` on its first line, then the time and the backup's name or the reason.

Both set-ups write the same format (checked: the Docker tools read and verify a backup made on Windows), so a backup can be taken to the other kind of set-up.

> ⚠ **The backups contain patient data** (names, contact details, medical notes). They stay on the same machine as the database; `backups/` is git-ignored. See [Copies off the machine](#copies-off-the-machine).

---

## Docker

Nothing to set up: `docker compose up` starts the `backup` service next to MySQL. Settings in `.env` (defaults in `.env.docker.example`):

| Setting | Default | Meaning |
|---|---|---|
| `BACKUP_DIR` | `./backups` | Folder on this machine |
| `BACKUP_TIME` | `02:00` | When the nightly backup runs |
| `BACKUP_TZ` | `Asia/Kolkata` | The time zone of `BACKUP_TIME` |
| `BACKUP_KEEP_DAILY`, `BACKUP_KEEP_WEEKLY` | `7`, `4` | How many to keep |

```powershell
# Did it work? (or open backups\LAST_RUN.txt)
docker compose exec backup bash /scripts/entrypoint.sh list

# A backup now (e.g. before an upgrade)
docker compose exec backup bash /scripts/entrypoint.sh backup

# Restore - replaces the database AND the uploaded images
docker compose stop backend
docker compose exec backup bash /scripts/entrypoint.sh restore 2026-10-08_020000 --yes
docker compose start backend
```

The service logs to `docker compose logs backup`.

---

## Windows (normal MySQL installation)

The scripts are in `tools\backup\`. They need Windows PowerShell 5.1 (built into Windows 10/11) and the MySQL Server installation (`mysqldump.exe`, `mysql.exe`, `mysql_config_editor.exe` — found automatically under `C:\Program Files\MySQL\MySQL Server 8.0\bin`).

### Set it up once

1. **Store the MySQL login** (asks for the root password once; stored for your Windows user, so no password is in any script):

   ```powershell
   & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql_config_editor.exe" set --login-path=hospitalerp-backup --user=root --password
   ```

2. **Take the first backup by hand** and check it (from the repository folder):

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File tools\backup\backup-windows.ps1
   Get-Content C:\db-backups\hospital-erp\LAST_RUN.txt        # first line: OK
   Get-ChildItem C:\db-backups\hospital-erp                    # a folder like 2026-10-08_103000
   ```

   - The database isn't called `hospital_erp`? Add `-Database <name>`.
   - `mysqldump.exe not found`? Add `-MySqlBin "C:\path\to\MySQL\bin"`.
   - The app keeps images somewhere else than `hospitalERP\uploads` (`UPLOAD_DIR` in `.env`)? Add `-UploadsDir <folder>`. (The scheduled task uses the default; for another folder, edit the task's arguments in Task Scheduler.)

3. **Register the nightly task** (no administrator rights needed):

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File tools\backup\install-backup-task.ps1 -DryRun   # shows what it would do
   powershell -NoProfile -ExecutionPolicy Bypass -File tools\backup\install-backup-task.ps1
   ```

   It runs every day at 02:00 as you. If the laptop was off or you weren't logged on, it runs as soon as possible afterwards. Another time: `-At 23:30`. Remove it: `-Remove`. If you move the repository folder, run the installer again (the task points to the script).

4. **The next morning**, check `C:\db-backups\hospital-erp\LAST_RUN.txt` (or Task Scheduler → "HospitalERP nightly backup" → Last Run Result `0x0`).

### Restore

```powershell
# 1. Stop the app (Ctrl+C in the window where it runs)
# 2. See the backups
powershell -NoProfile -ExecutionPolicy Bypass -File tools\backup\restore-windows.ps1
# 3. Restore one - replaces the database AND the uploaded images
powershell -NoProfile -ExecutionPolicy Bypass -File tools\backup\restore-windows.ps1 -Name 2026-10-08_020000 -Yes
# 4. Start the app again
```

The checksums are checked first; a damaged backup is refused before anything is changed.

---

## Copies off the machine

A backup on the same disk doesn't survive a dead disk, theft or ransomware. **Regularly copy the newest backup folder somewhere else** — and since it holds patient data:

- **Encrypt the copy**: e.g. a 7-Zip archive with AES-256 and a strong password, or a BitLocker-encrypted USB drive. Keep the password somewhere safe (without it the copy is useless).
- **Never** send backups by email or chat, and don't put unencrypted copies in a cloud folder.
- Delete old copies you no longer need.

## Practise a restore (e.g. once a month)

A backup you have never restored is a hope, not a backup. Restore the newest one into a **separate test database**, so nothing real is touched:

```powershell
# Windows
powershell -NoProfile -ExecutionPolicy Bypass -File tools\backup\restore-windows.ps1 -Name <newest> -Yes `
    -Database hospital_erp_restoretest -UploadsDir C:\temp\uploads-restoretest
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --login-path=hospitalerp-backup -e "SELECT COUNT(*) FROM hospital_erp_restoretest.users; DROP DATABASE hospital_erp_restoretest;"

# Docker
docker compose exec -e DB_NAME=hospital_erp_restoretest -e UPLOADS_DIR=/tmp/uploads-restoretest backup bash /scripts/entrypoint.sh restore <newest> --yes
docker compose exec backup bash -c 'mysql -h mysql -uroot -e "SELECT COUNT(*) FROM hospital_erp_restoretest.users; DROP DATABASE hospital_erp_restoretest;"'
```

The count should match the number of accounts in the app.

## When LAST_RUN.txt says FAILED

| Reason (third line) | What to do |
|---|---|
| `Access denied for user ...` | The root password changed. Docker: `DB_ROOT_PASSWORD` in `.env`. Windows: store the login again (setup step 1). |
| `mysqldump not found` / `mysqldump.exe not found` | Windows: pass `-MySqlBin` (and register the task again). |
| `Can't connect to MySQL server` | MySQL wasn't running at that time. Start it; the next night tries again, or run a backup by hand. |
| `the database dump is incomplete` | The dump stopped half-way (e.g. MySQL restarted). Run a backup by hand; if it repeats, check the MySQL error log. |
| `could not archive the uploads` | The uploads folder can't be read. Check its permissions / path. |
| `a backup named ... already exists` | Two backups in the same second — just run it again. |

Disk space: each backup is roughly the size of the compressed database plus the images; about 11 are kept.

## Before an upgrade

Take a backup by hand first (Docker: `entrypoint.sh backup`; Windows: `backup-windows.ps1`). For the one-time move to Flyway on the old database, follow [MYSQL_FLYWAY_UPGRADE.md](MYSQL_FLYWAY_UPGRADE.md) — it has its own backup step.
