<#
.SYNOPSIS
    Backs up the HospitalERP MySQL database and the uploaded images on Windows (docs/BACKUPS.md).

.DESCRIPTION
    One backup = one folder <BackupDir>\yyyy-MM-dd_HHmmss with hospital_erp.sql.gz (mysqldump), uploads.tar.gz,
    SHA256SUMS and manifest.txt - the same layout as the Docker backups. It is written as .in-progress-... and
    renamed only when it is complete and checked (the archives are readable, the dump ends with "Dump completed").
    LAST_RUN.txt says OK or FAILED (with the reason); backup.log keeps a line per step. After a good backup the old
    ones are removed: the 7 newest stay, plus the newest of each of the last 4 weeks.

    The MySQL login is read from a login path stored once with MySQL's own tool - no password in this script:
        mysql_config_editor set --login-path=hospitalerp-backup --user=root --password
    install-backup-task.ps1 runs this script every night.

.EXAMPLE
    .\backup-windows.ps1                      # one backup now, with the defaults
.EXAMPLE
    .\backup-windows.ps1 -PruneOnly           # only remove old backups
#>
[CmdletBinding()]
param(
    [string]$BackupDir = 'C:\db-backups\hospital-erp',
    [string]$Database = 'hospital_erp',
    [string]$LoginPath = 'hospitalerp-backup',
    # Where the app keeps uploaded images; empty = hospitalERP\uploads in this repository (the app's default)
    [string]$UploadsDir = '',
    # Folder with mysqldump.exe; found automatically when empty
    [string]$MySqlBin = '',
    # Full path of the dump tool; overrides -MySqlBin (the tests use a stand-in)
    [string]$MySqlDump = '',
    [int]$KeepDaily = 7,
    [int]$KeepWeekly = 4,
    [switch]$PruneOnly
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'BackupCommon.ps1')
# Windows PowerShell 5.1 has no $PSScriptRoot in parameter defaults, so this default is set here
if (-not $UploadsDir) { $UploadsDir = Join-Path $PSScriptRoot '..\..\hospitalERP\uploads' }

# Keeps the $KeepDaily newest backups and the newest backup of each of the $KeepWeekly most recent ISO weeks
function Remove-OldBackups([string]$Root, [int]$Daily, [int]$Weekly) {
    $keep = @{}
    $weeks = @{}
    $index = 0
    $backups = Get-BackupFolders $Root
    foreach ($backup in $backups) {
        if ($index -lt $Daily) { $keep[$backup.Name] = $true }
        $week = Get-IsoWeekKey ([datetime]::ParseExact($backup.Name.Substring(0, 10), 'yyyy-MM-dd', $null))
        if (-not $weeks.ContainsKey($week) -and $weeks.Count -lt $Weekly) {
            $weeks[$week] = $true # the newest backup of this week
            $keep[$backup.Name] = $true
        }
        $index++
    }
    foreach ($backup in $backups) {
        if (-not $keep.ContainsKey($backup.Name)) {
            Remove-Item -LiteralPath $backup.FullName -Recurse -Force
            Write-Log "removed old backup $($backup.Name)"
        }
    }
}

function Write-Log([string]$Message) {
    $line = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss') + ' ' + $Message
    Write-Host $line
    [IO.File]::AppendAllText((Join-Path $BackupDir 'backup.log'), $line + [Environment]::NewLine,
        (New-Object Text.UTF8Encoding $false))
}

# Dot-sourced (e.g. by the tests): only define the functions
if ($MyInvocation.InvocationName -eq '.') { return }

New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
$BackupDir = (Resolve-Path -LiteralPath $BackupDir).Path

if ($PruneOnly) {
    Remove-OldBackups $BackupDir $KeepDaily $KeepWeekly
    exit 0
}

$stamp = Get-Date -Format 'yyyy-MM-dd_HHmmss'
$work = Join-Path $BackupDir ".in-progress-$stamp"
$final = Join-Path $BackupDir $stamp

function Stop-WithFailure([string]$Reason) {
    Write-Log "FAILED: $Reason"
    if (Test-Path -LiteralPath $work) { Remove-Item -LiteralPath $work -Recurse -Force }
    Write-LastRun $BackupDir 'FAILED' $Reason
    exit 1
}

try {
    if ($MySqlDump) {
        $dumpTool = Find-MySqlTool 'mysqldump' $MySqlDump
    } elseif ($MySqlBin) {
        $dumpTool = Find-MySqlTool 'mysqldump' (Join-Path $MySqlBin 'mysqldump.exe')
    } else {
        $dumpTool = Find-MySqlTool 'mysqldump' ''
    }
} catch {
    Stop-WithFailure $_.Exception.Message
}

if (Test-Path -LiteralPath $work) { Remove-Item -LiteralPath $work -Recurse -Force }
New-Item -ItemType Directory -Path $work | Out-Null

Write-Log "dumping database $Database"
$sqlFile = Join-Path $work 'hospital_erp.sql'
$result = Invoke-MySqlTool $dumpTool @("--login-path=$LoginPath", '--single-transaction', '--routines', '--triggers',
    '--events', '--no-tablespaces', '--set-gtid-purged=OFF', '--default-character-set=utf8mb4',
    "--result-file=$sqlFile", $Database)
if ($result.ExitCode -ne 0) {
    $reason = $result.Error -replace '\s+', ' '
    if ($reason.Length -gt 300) { $reason = $reason.Substring(0, 300) }
    Stop-WithFailure "mysqldump (exit $($result.ExitCode)): $reason"
}
if (-not (Test-Path -LiteralPath $sqlFile) -or (Get-Item -LiteralPath $sqlFile).Length -eq 0) {
    Stop-WithFailure 'mysqldump wrote no dump'
}
try {
    Compress-GzipFile $sqlFile "$sqlFile.gz"
    Remove-Item -LiteralPath $sqlFile
    $lastLine = Get-GzipLastLine "$sqlFile.gz"
} catch {
    Stop-WithFailure "could not compress or read the dump: $($_.Exception.Message)"
}
if ($lastLine -notlike '-- Dump completed*') {
    Stop-WithFailure "the database dump is incomplete (no 'Dump completed' line)"
}

$UploadsDir = [IO.Path]::GetFullPath($UploadsDir)
$uploadsArchive = Join-Path $work 'uploads.tar.gz'
if (Test-Path -LiteralPath $UploadsDir) {
    Write-Log "archiving uploads from $UploadsDir"
    $tar = Get-TarPath
    & $tar -czf $uploadsArchive -C $UploadsDir .
    if ($LASTEXITCODE -ne 0) { Stop-WithFailure "could not archive the uploads in $UploadsDir" }
    & $tar -tzf $uploadsArchive | Out-Null
    if ($LASTEXITCODE -ne 0) { Stop-WithFailure 'the uploads archive is damaged' }
} else {
    Write-Log "no uploads folder at $UploadsDir - only the database is backed up"
}

Write-Checksums $work
$files = @(Get-ChildItem -LiteralPath $work -Filter '*.gz' | Sort-Object Name | ForEach-Object {
    '  {0,-20} {1,10} bytes' -f $_.Name, $_.Length
})
$uploadsLine = if (Test-Path -LiteralPath $uploadsArchive) { $UploadsDir } else { 'none' }
Write-Utf8Lines (Join-Path $work 'manifest.txt') (@(
    "HospitalERP backup $stamp",
    "created:   $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss')) ($([TimeZoneInfo]::Local.Id))",
    "database:  $Database (login path $LoginPath, $dumpTool)",
    "uploads:   $uploadsLine",
    'files:') + $files + @('restore:   see docs/BACKUPS.md'))

if (Test-Path -LiteralPath $final) { Stop-WithFailure "a backup named $stamp already exists" }
Rename-Item -LiteralPath $work -NewName $stamp
Write-Log "OK: $final"
Write-LastRun $BackupDir 'OK' $stamp

Remove-OldBackups $BackupDir $KeepDaily $KeepWeekly
exit 0
