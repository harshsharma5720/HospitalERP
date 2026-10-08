#!/usr/bin/env bash
# Tests the backup scripts against a throw-away MySQL 8 container (docs/MYSQL_BACKUPS_PLAN.md, step E.1):
# backup -> damage the data -> restore -> data back; a refused restore; a failing backup; retention.
# Needs Docker. Run from anywhere:  bash docker/backup/test-backup.sh
set -euo pipefail
export MSYS_NO_PATHCONV=1 # Git Bash on Windows: don't turn /paths in docker arguments into C:/... paths

here="$(cd "$(dirname "$0")" && pwd)"
work="$(mktemp -d)"
net="hbtest-net-$$"
db="hbtest-db-$$"
pw="test-root-pw"
mkdir -p "$work/backups" "$work/uploads/profileImages" "$work/retention"

cleanup() {
    docker rm -f "$db" > /dev/null 2>&1 || true
    docker network rm "$net" > /dev/null 2>&1 || true
    rm -rf "$work"
}
trap cleanup EXIT

host_path() { (cd "$1" && (pwd -W 2> /dev/null || pwd)); } # a path docker understands (Windows: D:/...)
check() { if eval "$2"; then echo "  ok   $1"; else echo "  FAIL $1"; exit 1; fi; }

# The backup container: the scripts as in docker-compose.yml, against the test database
run_backup() { # [env overrides...] -- mode args...
    local envs=()
    while [ "$1" != "--" ]; do envs+=(-e "$1"); shift; done
    shift
    docker run --rm --network "$net" -e DB_HOST="$db" -e MYSQL_PWD="$pw" -e TZ=Asia/Kolkata "${envs[@]}" \
        -v "$(host_path "$here"):/scripts:ro" -v "$(host_path "$work/backups"):/backups" \
        -v "$(host_path "$work/uploads"):/uploads" -v "$(host_path "$work/retention"):/retention" \
        mysql:8.0 bash /scripts/entrypoint.sh "$@"
}
sql() { docker exec -e MYSQL_PWD="$pw" "$db" mysql -uroot -N --default-character-set=utf8mb4 hospital_erp -e "$1"; }

echo "starting a throw-away MySQL 8 ..."
docker network create "$net" > /dev/null
docker run -d --name "$db" --network "$net" -e MYSQL_ROOT_PASSWORD="$pw" -e MYSQL_DATABASE=hospital_erp \
    mysql:8.0 --innodb-buffer-pool-size=32M --performance-schema=OFF > /dev/null
for _ in $(seq 1 90); do # TCP answers only once the real server runs (not the init one)
    docker exec -e MYSQL_PWD="$pw" "$db" mysqladmin ping -h 127.0.0.1 -uroot --silent > /dev/null 2>&1 && break
    sleep 2
done
sql "CREATE TABLE patient (id INT PRIMARY KEY, name VARCHAR(100)) CHARACTER SET utf8mb4;
     INSERT INTO patient VALUES (1, 'Asha'), (2, 'Ravi Kumar'), (3, 'Ñandú ✓ आशा');
     CREATE TABLE appointment (id INT PRIMARY KEY, patient_id INT, FOREIGN KEY (patient_id) REFERENCES patient(id));
     INSERT INTO appointment VALUES (10, 1), (11, 3);"
printf 'image-bytes-1' > "$work/uploads/profileImages/a.jpg"

echo "1. a backup"
run_backup -- backup
name="$(ls "$work/backups" | grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{6}$')"
check "one backup folder ($name)" '[ "$(echo "$name" | wc -l)" -eq 1 ]'
for f in hospital_erp.sql.gz uploads.tar.gz manifest.txt SHA256SUMS; do
    check "  contains $f" '[ -s "$work/backups/$name/$f" ]'
done
check "LAST_RUN.txt says OK" '[ "$(head -n 1 "$work/backups/LAST_RUN.txt")" = OK ]'
check "no half-written folder left" '! ls -a "$work/backups" | grep -q in-progress'

echo "2. damage the data, then restore"
sql "DELETE FROM appointment WHERE id = 11; DELETE FROM patient WHERE id = 3; UPDATE patient SET name = 'changed' WHERE id = 1;"
rm "$work/uploads/profileImages/a.jpg"
printf 'not in the backup' > "$work/uploads/profileImages/new.jpg"
run_backup -- restore "$name" --yes
check "patients back (incl. non-English names)" \
    '[ "$(sql "SELECT GROUP_CONCAT(name ORDER BY id SEPARATOR \"|\") FROM patient")" = "Asha|Ravi Kumar|Ñandú ✓ आशा" ]'
check "appointments back" '[ "$(sql "SELECT COUNT(*) FROM appointment")" = 2 ]'
check "image back with its content" '[ "$(cat "$work/uploads/profileImages/a.jpg")" = image-bytes-1 ]'
check "images that weren't in the backup are gone" '[ ! -e "$work/uploads/profileImages/new.jpg" ]'

echo "3. a restore needs --yes"
sql "UPDATE patient SET name = 'kept' WHERE id = 2;"
check "refused without --yes" '! run_backup -- restore "$name" > /dev/null 2>&1'
check "nothing changed" '[ "$(sql "SELECT name FROM patient WHERE id = 2")" = kept ]'
check "unknown backup refused" '! run_backup -- restore 2001-01-01_000000 --yes > /dev/null 2>&1'

echo "4. a failing backup"
sleep 1 # a new folder name (seconds)
check "fails with a wrong password" '! run_backup MYSQL_PWD=wrong -- backup > /dev/null 2>&1'
check "LAST_RUN.txt says FAILED" '[ "$(head -n 1 "$work/backups/LAST_RUN.txt")" = FAILED ]'
check "  ... with the reason" 'grep -q "Access denied" "$work/backups/LAST_RUN.txt"'
check "still only the first backup" '[ "$(ls "$work/backups" | grep -cE "^[0-9]{4}-")" -eq 1 ]'
check "no half-written folder left" '! ls -a "$work/backups" | grep -q in-progress'

echo "5. retention: 60 nightly backups up to Wed 2026-10-07"
for i in $(seq 0 59); do
    mkdir "$work/retention/$(date -d "2026-10-07 -$i days" +%Y-%m-%d)_020000"
done
mkdir "$work/retention/notes" "$work/retention/.in-progress-2026-10-08_020000"
docker run --rm -e TZ=Asia/Kolkata -v "$(host_path "$here"):/scripts:ro" -v "$(host_path "$work/retention"):/retention" \
    mysql:8.0 bash /scripts/prune.sh /retention > /dev/null
kept="$(ls "$work/retention" | grep -E '^[0-9]{4}-' | sort -r | tr '\n' ' ')"
expected="2026-10-07_020000 2026-10-06_020000 2026-10-05_020000 2026-10-04_020000 2026-10-03_020000 2026-10-02_020000 2026-10-01_020000 2026-09-27_020000 2026-09-20_020000 "
check "7 newest + newest of the last 4 weeks (Sun 27 Sep, Sun 20 Sep)" '[ "$kept" = "$expected" ]'
check "other folders untouched" '[ -d "$work/retention/notes" ] && [ -d "$work/retention/.in-progress-2026-10-08_020000" ]'

echo "ALL BACKUP TESTS PASSED"
