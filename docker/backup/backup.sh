#!/usr/bin/env bash
# One backup of the database and the uploaded images (docs/MYSQL_BACKUPS_PLAN.md):
#   $BACKUP_ROOT/<yyyy-MM-dd_HHmmss>/  hospital_erp.sql.gz  uploads.tar.gz  manifest.txt  SHA256SUMS
# Written under a temporary name and renamed only when it is complete and checked; then old backups are
# pruned (prune.sh). The result - OK or FAILED - goes to $BACKUP_ROOT/LAST_RUN.txt.
# The password comes from MYSQL_PWD (read by the mysql tools), never from the command line.
set -euo pipefail

BACKUP_ROOT="${BACKUP_ROOT:-/backups}"
DB_HOST="${DB_HOST:-mysql}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-hospital_erp}"
DB_USER="${DB_USER:-root}"
UPLOADS_DIR="${UPLOADS_DIR:-/uploads}"
SCRIPTS="$(cd "$(dirname "$0")" && pwd)"

stamp="$(date +%Y-%m-%d_%H%M%S)"
work="$BACKUP_ROOT/.in-progress-$stamp"
final="$BACKUP_ROOT/$stamp"

log() { echo "$(date '+%Y-%m-%d %H:%M:%S') backup: $*"; }

last_run() { # OK|FAILED, message
    printf '%s\n%s\n%s\n' "$1" "$(date '+%Y-%m-%d %H:%M:%S %Z')" "$2" > "$BACKUP_ROOT/LAST_RUN.txt"
}

fail() {
    log "FAILED: $1"
    rm -rf "$work"
    last_run FAILED "$1"
    exit 1
}

mkdir -p "$BACKUP_ROOT"
rm -rf "$work"
mkdir "$work"

log "dumping database $DB_NAME from $DB_HOST:$DB_PORT"
if ! mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" --single-transaction --routines --triggers --events \
        --no-tablespaces --set-gtid-purged=OFF --default-character-set=utf8mb4 "$DB_NAME" 2> "$work/dump.err" \
        | gzip -c > "$work/hospital_erp.sql.gz"; then
    fail "mysqldump: $(head -c 300 "$work/dump.err" | tr '\n' ' ')"
fi
rm -f "$work/dump.err"
gzip -t "$work/hospital_erp.sql.gz" || fail "the database archive is damaged"
case "$(gzip -dc "$work/hospital_erp.sql.gz" | tail -n 1)" in
    "-- Dump completed"*) ;;
    *) fail "the database dump is incomplete (no 'Dump completed' line)" ;;
esac

if [ -d "$UPLOADS_DIR" ]; then
    log "archiving uploads from $UPLOADS_DIR"
    tar -czf "$work/uploads.tar.gz" -C "$UPLOADS_DIR" . || fail "could not archive the uploads in $UPLOADS_DIR"
    gzip -t "$work/uploads.tar.gz" || fail "the uploads archive is damaged"
else
    log "no uploads folder at $UPLOADS_DIR - only the database is backed up"
fi

(cd "$work" && sha256sum -- *.gz > SHA256SUMS)
{
    echo "HospitalERP backup $stamp"
    echo "created:   $(date '+%Y-%m-%d %H:%M:%S %Z')"
    echo "database:  $DB_NAME on $DB_HOST:$DB_PORT ($(mysqldump --version | head -n 1))"
    echo "uploads:   $([ -f "$work/uploads.tar.gz" ] && echo "$UPLOADS_DIR" || echo "none")"
    echo "files:"
    (cd "$work" && for f in *.gz; do printf '  %-20s %10s bytes\n' "$f" "$(stat -c %s "$f")"; done)
    echo "restore:   see docs/BACKUPS.md"
} > "$work/manifest.txt"

[ -e "$final" ] && fail "a backup named $stamp already exists"
mv "$work" "$final"
log "OK: $final"
last_run OK "$stamp"

bash "$SCRIPTS/prune.sh" "$BACKUP_ROOT"
