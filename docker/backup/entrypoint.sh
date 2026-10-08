#!/usr/bin/env bash
# The backup container (docs/MYSQL_BACKUPS_PLAN.md, docs/BACKUPS.md):
#   schedule               (default) one backup every night at BACKUP_TIME (HH:MM, time zone TZ)
#   backup                 one backup now
#   restore <name> --yes   restore a backup (stop the backend first)
#   list                   the backups and the result of the last run
set -euo pipefail

SCRIPTS="$(cd "$(dirname "$0")" && pwd)"
BACKUP_ROOT="${BACKUP_ROOT:-/backups}"

list_backups() {
    find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' 2>/dev/null \
        | grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{6}$' | sort -r || true
}

case "${1:-schedule}" in
    schedule)
        at="${BACKUP_TIME:-02:00}"
        if ! [[ "$at" =~ ^([01][0-9]|2[0-3]):[0-5][0-9]$ ]]; then
            echo "backup: BACKUP_TIME must be HH:MM, not '$at'" >&2
            exit 2
        fi
        echo "backup: every night at $at ($(date +%Z)), into $BACKUP_ROOT"
        while true; do
            next="$(date -d "today $at" +%s)"
            [ "$next" -le "$(date +%s)" ] && next="$(date -d "tomorrow $at" +%s)"
            # Short naps, so a computer that slept through the time still backs up soon after waking
            while [ "$(date +%s)" -lt "$next" ]; do
                remaining=$(( next - $(date +%s) ))
                sleep $(( remaining < 300 ? (remaining > 0 ? remaining : 1) : 300 ))
            done
            bash "$SCRIPTS/backup.sh" || true # a failed night is in LAST_RUN.txt; tomorrow is the next try
        done
        ;;
    backup)
        exec bash "$SCRIPTS/backup.sh"
        ;;
    restore)
        shift
        exec bash "$SCRIPTS/restore.sh" "$@"
        ;;
    list)
        list_backups
        echo "---"
        cat "$BACKUP_ROOT/LAST_RUN.txt" 2>/dev/null || echo "no backup has run yet"
        ;;
    *)
        echo "usage: entrypoint.sh [schedule | backup | restore <name> --yes | list]" >&2
        exit 2
        ;;
esac
