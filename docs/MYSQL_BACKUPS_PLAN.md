# Automatic MySQL backups — plan

**Branch:** `feature/mysql-backups` (from `main` = c67e5dd)
**Why:** nothing backs up the database or the uploaded profile images. A broken disk, a wrong `docker compose down -v` or a bad upgrade would lose every patient record. The only backup today is the manual one in the [MySQL runbook](MYSQL_FLYWAY_UPGRADE.md).
**Idea list:** [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md#not-implemented-yet) — "not implemented yet" #4.

## Decisions (2026-10-08)

| Question | Decision |
|---|---|
| Where | **Docker and Windows:** a backup container in `docker-compose`, and a PowerShell script with a Windows Task Scheduler entry for the normal MySQL installation on the other laptop (where the real data is). |
| Schedule and retention | **Nightly at 02:00**; keep the **7 newest** backups and the newest backup of each of the **last 4 weeks** (about 11). |
| Contents | **Database + uploaded images:** the MySQL dump and the uploads folder, so a restore brings everything back. |
| Protection | **Compressed** (gzip / zip) in a backups folder on the same machine — as protected as the database itself. The guide says to encrypt any copy taken off the machine. |

## How it works

- **One backup = one folder** named after its time, e.g. `2026-10-08_020000/` (to the second, so two manual backups never collide), with `hospital_erp.sql.gz` (the dump), the uploads archive, `SHA256SUMS` (checked before a restore) and a readable `manifest.txt`. It is written under a temporary name and only renamed when complete, so a half-written backup never looks like a good one.
- **Checked before it counts:** the archive must be readable and the dump must end with MySQL's "Dump completed" line.
- **`LAST_RUN.txt`** in the backups folder says whether the last run worked (OK / FAILED, time, message) — the first place to look.
- **Old backups are removed** after each successful run (never after a failed one): the 7 newest stay, plus the newest of each of the last 4 weeks.
- **Restore** is a separate command that must be confirmed (`--yes`); the guide says to stop the app first.
- **Not in git:** the backups folder is git-ignored — the files contain patient data.

## Steps

| Step | Work | Checked by |
|---|---|---|
| E.1 | **Backup and restore scripts** for Docker (bash, run in a `mysql:8.0` container): backup, verify, manifest, `LAST_RUN.txt`, retention; restore. | A test script that runs them against throw-away MySQL containers: backup → damage the data → restore → data back; retention on 60 days of fake backups; a failing backup leaves nothing half-written |
| E.2 | **`backup` service in `docker-compose.yml`:** nightly schedule (time zone from `.env`), `./backups` folder, uploads volume; `.env.docker.example`, `.gitignore`. | `docker compose config`; a run of the service against the compose MySQL |
| E.3 | **Windows:** `backup-windows.ps1` (mysqldump with a stored login, gzip, zip of the uploads, the same checks and retention) and `install-backup-task.ps1` (Task Scheduler, 02:00, runs later if the laptop was off). | Tests with synthetic data and a stand-in for mysqldump (no MySQL installed on this laptop); a real run is a step in the guide |
| E.4 | **Docs:** a backup and restore guide (`docs/BACKUPS.md`), README, MySQL runbook link, idea list status. | — |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| E.0 Plan + decisions | ✅ done | 2026-10-08 | This file. |
| E.1 Backup and restore scripts | ✅ done | 2026-10-08 | `docker/backup/` (bash, run in the `mysql:8.0` image — its `mysqldump` 8.0 matches the server; `tar`, `gzip`, `sha256sum`, GNU `date` and the time-zone data are there): `backup.sh` (mysqldump `--single-transaction` of `hospital_erp` → gzip, uploads → tar.gz, checks: archives readable, dump ends with "-- Dump completed"; `SHA256SUMS` + `manifest.txt`; written as `.in-progress-…` and renamed only when complete; `LAST_RUN.txt` OK / FAILED with the reason; then `prune.sh`), `prune.sh` (7 newest + newest of each of the 4 most recent ISO weeks; only `yyyy-MM-dd_HHmmss` folders), `restore.sh` (`<name> --yes`; checks the checksums, drops and re-creates the database, loads the dump, replaces the uploads), `entrypoint.sh` (`schedule` — nightly at `BACKUP_TIME`, waking every 5 minutes so a computer that slept still backs up soon after; `backup`; `restore`; `list`). Password only via `MYSQL_PWD`. Folder names to the second (not `HHmm`), so two manual backups never collide. `.gitattributes` keeps LF line endings. `test-backup.sh` runs them against a throw-away MySQL 8 container: a backup (all files, OK, nothing half-written); damaged data (deleted / changed rows incl. non-English names, a deleted and an extra image) → restore → all back; restore refused without `--yes` and for an unknown name; a wrong password → FAILED with "Access denied", no new or half-written folder; retention on 60 nightly fake backups → exactly the 7 newest + 27 Sep + 20 Sep, other folders untouched. All checks pass; the test leaves no containers behind. |
