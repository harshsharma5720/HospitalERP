<#
.SYNOPSIS
    Restores a HospitalERP backup on Windows (docs/BACKUPS.md): the database is dropped and re-created from the dump,
    and the uploads folder is replaced by the backup's images.

.DESCRIPTION
    Stop the app first. The backup's checksums are checked before anything is changed. Without -Yes nothing happens.
    The MySQL login comes from the same login path as the backup (mysql_config_editor).

.EXAMPLE
    .\restore-windows.ps1                                  # lists the backups
.EXAMPLE
    .\restore-windows.ps1 -Name 2026-10-08_020000 -Yes
#>
[CmdletBinding()]
param(
    [string]$Name = '',
    [switch]$Yes,
    [string]$BackupDir = 'C:\db-backups\hospital-erp',
    [string]$Database = 'hospital_erp',
    [string]$LoginPath = 'hospitalerp-backup',
    # Where the app keeps uploaded images; empty = hospitalERP\uploads in this repository (the app's default)
    [string]$UploadsDir = '',
    # Folder with mysql.exe; found automatically when empty
    [string]$MySqlBin = '',
    # Full path of the client; overrides -MySqlBin (the tests use a stand-in)
    [string]$MySql = ''
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'BackupCommon.ps1')
# Windows PowerShell 5.1 has no $PSScriptRoot in parameter defaults, so this default is set here
if (-not $UploadsDir) { $UploadsDir = Join-Path $PSScriptRoot '..\..\hospitalERP\uploads' }

$folder = Join-Path $BackupDir $Name
if (-not $Name -or $Name -notmatch $script:BackupNamePattern -or -not (Test-Path -LiteralPath $folder)) {
    if ($Name) { Write-Host "There is no backup '$Name' in $BackupDir." }
    Write-Host "Backups in ${BackupDir}:"
    Get-BackupFolders $BackupDir | ForEach-Object { Write-Host "  $($_.Name)" }
    if (Test-Path -LiteralPath (Join-Path $BackupDir 'LAST_RUN.txt')) {
        Write-Host 'Last run:'
        Get-Content -LiteralPath (Join-Path $BackupDir 'LAST_RUN.txt') | ForEach-Object { Write-Host "  $_" }
    }
    exit 2
}
if (-not $Yes) {
    Write-Host "This REPLACES database '$Database' and the uploaded images with backup $Name."
    Write-Host 'Stop the app first, then run again with -Yes.'
    exit 2
}

$problems = @(Get-ChecksumProblems $folder)
if ($problems.Count -gt 0) {
    Write-Host "Backup $Name can't be used: $($problems -join '; ')"
    exit 1
}

if ($MySql) {
    $client = Find-MySqlTool 'mysql' $MySql
} elseif ($MySqlBin) {
    $client = Find-MySqlTool 'mysql' (Join-Path $MySqlBin 'mysql.exe')
} else {
    $client = Find-MySqlTool 'mysql' ''
}

$sqlFile = Join-Path ([IO.Path]::GetTempPath()) "hospitalerp-restore-$Name.sql"
try {
    Write-Host "Re-creating database $Database ..."
    Expand-GzipFile (Join-Path $folder 'hospital_erp.sql.gz') $sqlFile
    $result = Invoke-MySqlTool $client @("--login-path=$LoginPath", '-e',
        "DROP DATABASE IF EXISTS ``$Database``; CREATE DATABASE ``$Database`` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;")
    if ($result.ExitCode -ne 0) { throw "mysql: $($result.Error)" }
    Write-Host 'Loading the dump ...'
    $result = Invoke-MySqlTool $client @("--login-path=$LoginPath", '--default-character-set=utf8mb4', $Database,
        '-e', ('source ' + ($sqlFile -replace '\\', '/')))
    if ($result.ExitCode -ne 0) { throw "mysql: $($result.Error)" }
} finally {
    Remove-Item -LiteralPath $sqlFile -ErrorAction SilentlyContinue
}

$archive = Join-Path $folder 'uploads.tar.gz'
if (Test-Path -LiteralPath $archive) {
    $UploadsDir = [IO.Path]::GetFullPath($UploadsDir)
    Write-Host "Replacing the uploads in $UploadsDir ..."
    New-Item -ItemType Directory -Force -Path $UploadsDir | Out-Null
    Get-ChildItem -LiteralPath $UploadsDir -Force | Remove-Item -Recurse -Force
    & (Get-TarPath) -xzf $archive -C $UploadsDir
    if ($LASTEXITCODE -ne 0) { throw "could not unpack $archive" }
}
Write-Host "Done - backup $Name is back. Start the app again."
exit 0
