<#
.SYNOPSIS
    Tests the Windows backup scripts (docs/MYSQL_BACKUPS_PLAN.md, step E.3) with stand-ins for mysqldump / mysql,
    so no MySQL is needed: backup, failures, ISO weeks, retention, restore, and the task installer (dry run).
    Run:  powershell -NoProfile -ExecutionPolicy Bypass -File tools\backup\test\test-backup-windows.ps1
#>
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$tools = Split-Path -Parent $here
$root = Join-Path ([IO.Path]::GetTempPath()) ('hb-wintest-' + [guid]::NewGuid().ToString('N').Substring(0, 8))
$backups = Join-Path $root 'backups'
$uploads = Join-Path $root 'uploads'
$script:failures = 0

function Check([string]$Name, [scriptblock]$Condition) {
    if (& $Condition) { Write-Host "  ok   $Name" } else { Write-Host "  FAIL $Name"; $script:failures++ }
}

# Runs one of the scripts in its own PowerShell, like Task Scheduler does; returns the exit code
function Invoke-Script([string]$Script, [string[]]$Arguments, [hashtable]$Environment = @{}) {
    foreach ($key in $Environment.Keys) { Set-Item -Path "env:$key" -Value $Environment[$key] }
    try {
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $tools $Script) @Arguments | Out-Null
        return $LASTEXITCODE
    } finally {
        foreach ($key in $Environment.Keys) { Remove-Item -Path "env:$key" -ErrorAction SilentlyContinue }
    }
}

function Get-Names { @(Get-ChildItem -LiteralPath $backups -Directory | ForEach-Object Name) }

New-Item -ItemType Directory -Force -Path (Join-Path $uploads 'profileImages') | Out-Null
[IO.File]::WriteAllText((Join-Path $uploads 'profileImages\a.jpg'), 'image-bytes-1')
$backupArgs = @('-BackupDir', $backups, '-UploadsDir', $uploads, '-MySqlDump', (Join-Path $here 'fake-mysqldump.ps1'))
$tar = Join-Path $env:SystemRoot 'System32\tar.exe'

try {
    Write-Host '1. a backup'
    $code = Invoke-Script 'backup-windows.ps1' $backupArgs
    $name = Get-Names | Where-Object { $_ -match '^\d{4}-\d{2}-\d{2}_\d{6}$' } | Select-Object -First 1
    $folder = Join-Path $backups "$name"
    Check 'exit code 0' { $code -eq 0 }
    Check "one backup folder ($name)" { @(Get-Names).Count -eq 1 -and $name }
    foreach ($file in 'hospital_erp.sql.gz', 'uploads.tar.gz', 'manifest.txt', 'SHA256SUMS') {
        Check "  contains $file" { (Test-Path -LiteralPath (Join-Path $folder $file)) }
    }
    Check '  no uncompressed dump left' { -not (Test-Path -LiteralPath (Join-Path $folder 'hospital_erp.sql')) }
    . (Join-Path $tools 'BackupCommon.ps1')
    Check '  the dump decompresses and ends with "Dump completed"' {
        (Get-GzipLastLine (Join-Path $folder 'hospital_erp.sql.gz')) -like '-- Dump completed*' }
    Check '  checksums match' { @(Get-ChecksumProblems $folder).Count -eq 0 }
    Check '  SHA256SUMS in sha256sum format' {
        (Get-Content -LiteralPath (Join-Path $folder 'SHA256SUMS') -TotalCount 1) -match '^[0-9a-f]{64}  hospital_erp\.sql\.gz$' }
    Check '  ... with LF line endings (Linux sha256sum, i.e. the Docker restore, reads it)' {
        -not [IO.File]::ReadAllText((Join-Path $folder 'SHA256SUMS')).Contains("`r") }
    Check '  the uploads archive holds the image' {
        (& $tar -tzf (Join-Path $folder 'uploads.tar.gz')) -match 'profileImages/a\.jpg' }
    Check 'LAST_RUN.txt says OK' { (Get-Content -LiteralPath (Join-Path $backups 'LAST_RUN.txt') -TotalCount 1) -eq 'OK' }
    Check 'backup.log written' { (Get-Content -LiteralPath (Join-Path $backups 'backup.log') -Raw) -match 'OK: ' }

    Write-Host '2. failures leave nothing half-written'
    Start-Sleep -Seconds 1
    $code = Invoke-Script 'backup-windows.ps1' $backupArgs @{ FAKE_DUMP_MODE = 'fail' }
    Check 'mysqldump error: exit code 1' { $code -eq 1 }
    Check '  LAST_RUN.txt says FAILED with the reason' {
        $lines = Get-Content -LiteralPath (Join-Path $backups 'LAST_RUN.txt')
        $lines[0] -eq 'FAILED' -and $lines[2] -match 'Access denied' }
    Start-Sleep -Seconds 1
    $code = Invoke-Script 'backup-windows.ps1' $backupArgs @{ FAKE_DUMP_MODE = 'incomplete' }
    Check 'incomplete dump: exit code 1' { $code -eq 1 }
    Check '  ... reason "incomplete"' { (Get-Content -LiteralPath (Join-Path $backups 'LAST_RUN.txt'))[2] -match 'incomplete' }
    Check 'still only the first backup, no .in-progress folder' { @(Get-Names).Count -eq 1 }
    $code = Invoke-Script 'backup-windows.ps1' @('-BackupDir', $backups, '-MySqlDump', 'C:\nowhere\mysqldump.exe')
    Check 'missing mysqldump: exit code 1' { $code -eq 1 }
    Check '  ... with the reason (and the default uploads folder works)' {
        (Get-Content -LiteralPath (Join-Path $backups 'LAST_RUN.txt'))[2] -match 'mysqldump not found at' }

    Write-Host '3. ISO weeks (as Linux "date +%G-%V")'
    Check '2026-10-07 -> 2026-41' { (Get-IsoWeekKey ([datetime]'2026-10-07')) -eq '2026-41' }
    Check '2026-01-01 (Thu) -> 2026-01' { (Get-IsoWeekKey ([datetime]'2026-01-01')) -eq '2026-01' }
    Check '2027-01-01 (Fri) -> 2026-53' { (Get-IsoWeekKey ([datetime]'2027-01-01')) -eq '2026-53' }
    Check '2024-12-30 (Mon) -> 2025-01' { (Get-IsoWeekKey ([datetime]'2024-12-30')) -eq '2025-01' }
    Check '2021-01-03 (Sun) -> 2020-53' { (Get-IsoWeekKey ([datetime]'2021-01-03')) -eq '2020-53' }

    Write-Host '4. retention: 60 nightly backups up to Wed 2026-10-07'
    $retention = Join-Path $root 'retention'
    0..59 | ForEach-Object {
        New-Item -ItemType Directory -Path (Join-Path $retention (([datetime]'2026-10-07').AddDays(-$_).ToString('yyyy-MM-dd') + '_020000')) | Out-Null
    }
    New-Item -ItemType Directory -Path (Join-Path $retention 'notes') | Out-Null
    $code = Invoke-Script 'backup-windows.ps1' @('-BackupDir', $retention, '-PruneOnly')
    $kept = (@(Get-ChildItem -LiteralPath $retention -Directory | Where-Object Name -match '^\d{4}-' |
        Sort-Object Name -Descending | ForEach-Object { $_.Name.Substring(0, 10) })) -join ' '
    Check 'exit code 0' { $code -eq 0 }
    Check '7 newest + newest of the last 4 weeks (Sun 27 Sep, Sun 20 Sep) - same as Docker' {
        $kept -eq '2026-10-07 2026-10-06 2026-10-05 2026-10-04 2026-10-03 2026-10-02 2026-10-01 2026-09-27 2026-09-20' }
    Check 'other folders untouched' { Test-Path -LiteralPath (Join-Path $retention 'notes') }

    Write-Host '5. restore'
    $log = Join-Path $root 'mysql.log'
    $loaded = Join-Path $root 'loaded.sql'
    $fakeMysql = Join-Path $here 'fake-mysql.ps1'
    $restoreArgs = @('-BackupDir', $backups, '-UploadsDir', $uploads, '-MySql', $fakeMysql)
    $fakeEnv = @{ FAKE_MYSQL_LOG = $log; FAKE_MYSQL_LOADED = $loaded }
    Remove-Item -LiteralPath (Join-Path $uploads 'profileImages\a.jpg')
    [IO.File]::WriteAllText((Join-Path $uploads 'profileImages\new.jpg'), 'not in the backup')
    $code = Invoke-Script 'restore-windows.ps1' ($restoreArgs + @('-Name', $name)) $fakeEnv
    Check 'refused without -Yes (exit 2), nothing changed' {
        $code -eq 2 -and -not (Test-Path -LiteralPath $log) -and (Test-Path -LiteralPath (Join-Path $uploads 'profileImages\new.jpg')) }
    $code = Invoke-Script 'restore-windows.ps1' ($restoreArgs + @('-Name', '2001-01-01_000000', '-Yes')) $fakeEnv
    Check 'unknown backup refused (exit 2)' { $code -eq 2 }
    $code = Invoke-Script 'restore-windows.ps1' ($restoreArgs + @('-Name', $name, '-Yes')) $fakeEnv
    $calls = @(Get-Content -LiteralPath $log)
    Check 'exit code 0' { $code -eq 0 }
    Check '  re-creates the database first, then loads the dump' {
        $calls.Count -eq 2 -and $calls[0] -match 'DROP DATABASE IF EXISTS `hospital_erp`; CREATE DATABASE `hospital_erp`' -and
        $calls[1] -match 'hospital_erp -e source ' }
    Check '  the dump it loads is the backed-up one' {
        (Get-Content -LiteralPath $loaded -Raw) -match "INSERT INTO patient VALUES \(1,'Asha'\)" }
    Check '  the image is back with its content' {
        [IO.File]::ReadAllText((Join-Path $uploads 'profileImages\a.jpg')) -eq 'image-bytes-1' }
    Check "  images that weren't in the backup are gone" { -not (Test-Path -LiteralPath (Join-Path $uploads 'profileImages\new.jpg')) }
    Check '  no temporary dump left' { -not (Test-Path -LiteralPath (Join-Path ([IO.Path]::GetTempPath()) "hospitalerp-restore-$name.sql")) }
    [IO.File]::AppendAllText((Join-Path $folder 'uploads.tar.gz'), 'x')
    Remove-Item -LiteralPath $log
    $code = Invoke-Script 'restore-windows.ps1' ($restoreArgs + @('-Name', $name, '-Yes')) $fakeEnv
    Check 'a damaged backup is refused before anything changes (exit 1)' { $code -eq 1 -and -not (Test-Path -LiteralPath $log) }

    Write-Host '6. the task installer (dry run - nothing is registered)'
    $output = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $tools 'install-backup-task.ps1') -DryRun -BackupDir $backups
    Check 'exit code 0' { $LASTEXITCODE -eq 0 }
    Check '  every day at 02:00, running backup-windows.ps1' {
        ($output -join "`n") -match 'every day at 02:00' -and ($output -join "`n") -match 'backup-windows\.ps1' }
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $tools 'install-backup-task.ps1') -DryRun -At '25:00' | Out-Null
    Check '  a wrong time is refused (exit 2)' { $LASTEXITCODE -eq 2 }
} finally {
    Remove-Item -LiteralPath $root -Recurse -Force -ErrorAction SilentlyContinue
}

if ($script:failures -gt 0) {
    Write-Host "$($script:failures) CHECK(S) FAILED"
    exit 1
}
Write-Host 'ALL WINDOWS BACKUP TESTS PASSED'
exit 0
