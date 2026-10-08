#!/usr/bin/env bash
# Restores a backup (docs/MYSQL_BACKUPS_PLAN.md): the database is dropped and re-created from the dump, and the
# uploads folder is replaced by the backup's images. Stop the backend first (docker compose stop backend).
# Usage: restore.sh <yyyy-MM-dd_HHmmss> --yes
set -euo pipefail

BACKUP_ROOT="${BACKUP_ROOT:-/backups}"
DB_HOST="${DB_HOST:-mysql}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-hospital_erp}"
DB_USER="${DB_USER:-root}"
UPLOADS_DIR="${UPLOADS_DIR:-/uploads}"

name="${1:-}"
confirm="${2:-}"
dir="$BACKUP_ROOT/$name"

if ! [[ "$name" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{6}$ ]] || [ ! -d "$dir" ]; then
    echo "restore: there is no backup '$name'. Backups in $BACKUP_ROOT:" >&2
    find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' \
        | grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{6}$' | sort -r >&2 || true
    exit 2
fi
if [ "$confirm" != "--yes" ]; then
    echo "restore: this REPLACES database '$DB_NAME' and the uploaded images with backup $name." >&2
    echo "Stop the backend first, then run again with --yes." >&2
    exit 2
fi

echo "restore: checking backup $name"
(cd "$dir" && sha256sum --check --quiet SHA256SUMS)

echo "restore: re-creating database $DB_NAME"
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -e \
    "DROP DATABASE IF EXISTS \`$DB_NAME\`; CREATE DATABASE \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
gzip -dc "$dir/hospital_erp.sql.gz" | mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" --default-character-set=utf8mb4 "$DB_NAME"

if [ -f "$dir/uploads.tar.gz" ]; then
    echo "restore: replacing the uploads in $UPLOADS_DIR"
    mkdir -p "$UPLOADS_DIR"
    find "$UPLOADS_DIR" -mindepth 1 -delete
    tar -xzf "$dir/uploads.tar.gz" -C "$UPLOADS_DIR"
fi
echo "restore: done - backup $name is back. Start the backend again (docker compose start backend)."
