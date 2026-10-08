#!/usr/bin/env bash
# Removes old backups (docs/MYSQL_BACKUPS_PLAN.md): keeps the KEEP_DAILY newest backups (default 7) and the
# newest backup of each of the KEEP_WEEKLY most recent ISO weeks that have one (default 4).
# Only folders named yyyy-MM-dd_HHmmss are considered; anything else in the folder is left alone.
# Usage: prune.sh [backup folder]
set -euo pipefail

root="${1:-${BACKUP_ROOT:-/backups}}"
keep_daily="${KEEP_DAILY:-7}"
keep_weekly="${KEEP_WEEKLY:-4}"

mapfile -t backups < <(find "$root" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' \
    | grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{6}$' | sort -r)

declare -A keep=()
declare -A weeks=()
index=0
for backup in "${backups[@]}"; do
    if [ "$index" -lt "$keep_daily" ]; then
        keep[$backup]=1
    fi
    week="$(date -d "${backup%%_*}" +%G-%V)"
    if [ -z "${weeks[$week]:-}" ] && [ "${#weeks[@]}" -lt "$keep_weekly" ]; then
        weeks[$week]=1   # the newest backup of this week
        keep[$backup]=1
    fi
    index=$((index + 1))
done

for backup in "${backups[@]}"; do
    if [ -z "${keep[$backup]:-}" ]; then
        rm -rf -- "${root:?}/$backup"
        echo "$(date '+%Y-%m-%d %H:%M:%S') prune: removed $backup"
    fi
done
