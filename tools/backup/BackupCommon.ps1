# Shared helpers of the Windows backup scripts (docs/BACKUPS.md, docs/MYSQL_BACKUPS_PLAN.md).
# Dot-sourced by backup-windows.ps1 and restore-windows.ps1; it only defines functions.
# Windows PowerShell 5.1 compatible; ASCII only.

$script:BackupNamePattern = '^\d{4}-\d{2}-\d{2}_\d{6}$'

# Backup folders (named yyyy-MM-dd_HHmmss), newest first
function Get-BackupFolders([string]$Root) {
    if (-not (Test-Path -LiteralPath $Root)) { return @() }
    @(Get-ChildItem -LiteralPath $Root -Directory |
        Where-Object { $_.Name -match $script:BackupNamePattern } |
        Sort-Object Name -Descending)
}

# ISO 8601 week, e.g. "2026-41" (the week's Thursday decides the year). .NET Framework has no ISOWeek class.
function Get-IsoWeekKey([datetime]$Date) {
    $daysSinceMonday = ([int]$Date.DayOfWeek + 6) % 7
    $thursday = $Date.Date.AddDays(3 - $daysSinceMonday)
    $week = [math]::Floor(($thursday.DayOfYear - 1) / 7) + 1
    '{0}-{1:D2}' -f $thursday.Year, [int]$week
}

# UTF-8 without BOM and LF line endings, so Linux tools (e.g. "sha256sum --check" in the Docker restore) read it too
function Write-Utf8Lines([string]$Path, [string[]]$Lines) {
    [IO.File]::WriteAllText($Path, (($Lines -join "`n") + "`n"), (New-Object Text.UTF8Encoding $false))
}

# LAST_RUN.txt: OK or FAILED, the time, and the backup name or the reason
function Write-LastRun([string]$Root, [string]$Status, [string]$Message) {
    $time = (Get-Date).ToString('yyyy-MM-dd HH:mm:ss') + ' (' + [TimeZoneInfo]::Local.Id + ')'
    Write-Utf8Lines (Join-Path $Root 'LAST_RUN.txt') @($Status, $time, $Message)
}

# mysqldump.exe / mysql.exe: the given path, else on PATH, else the newest "C:\Program Files\MySQL\MySQL Server *\bin"
function Find-MySqlTool([string]$Name, [string]$Given) {
    if ($Given) {
        if (Test-Path -LiteralPath $Given) { return (Resolve-Path -LiteralPath $Given).Path }
        throw "$Name not found at $Given"
    }
    $command = Get-Command "$Name.exe" -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $found = Get-ChildItem "C:\Program Files\MySQL\MySQL Server *\bin\$Name.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1
    if ($found) { return $found.FullName }
    throw "$Name.exe not found - is MySQL Server installed? Pass its path with -MySqlBin."
}

# Runs a MySQL tool and returns @{ ExitCode; Error }. Arguments with spaces are quoted.
# A .ps1 path is run with PowerShell - that is how the tests put in stand-ins for mysqldump / mysql.
function Invoke-MySqlTool([string]$Tool, [string[]]$Arguments) {
    $errorFile = [IO.Path]::GetTempFileName()
    try {
        $quoted = @($Arguments | ForEach-Object {
            if ($_ -match '[\s"]') { '"' + ($_ -replace '"', '\"') + '"' } else { $_ }
        })
        if ($Tool -like '*.ps1') {
            $all = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"' + $Tool + '"')) + $quoted
            $process = Start-Process -FilePath 'powershell.exe' -ArgumentList $all -NoNewWindow -Wait -PassThru `
                -RedirectStandardError $errorFile
        } else {
            $process = Start-Process -FilePath $Tool -ArgumentList $quoted -NoNewWindow -Wait -PassThru `
                -RedirectStandardError $errorFile
        }
        $errorText = [IO.File]::ReadAllText($errorFile).Trim()
        @{ ExitCode = $process.ExitCode; Error = $errorText }
    } finally {
        Remove-Item -LiteralPath $errorFile -ErrorAction SilentlyContinue
    }
}

# Windows' own tar.exe (Windows 10 1803+), not another tar that may be on PATH (e.g. Git's)
function Get-TarPath {
    $windowsTar = Join-Path $env:SystemRoot 'System32\tar.exe'
    if (Test-Path -LiteralPath $windowsTar) { return $windowsTar }
    return 'tar.exe'
}

function Compress-GzipFile([string]$Source, [string]$Target) {
    $in = [IO.File]::OpenRead($Source)
    try {
        $out = [IO.File]::Create($Target)
        try {
            $gzip = New-Object IO.Compression.GZipStream($out, [IO.Compression.CompressionMode]::Compress)
            try { $in.CopyTo($gzip) } finally { $gzip.Dispose() }
        } finally { $out.Dispose() }
    } finally { $in.Dispose() }
}

function Expand-GzipFile([string]$Source, [string]$Target) {
    $in = [IO.File]::OpenRead($Source)
    try {
        $gzip = New-Object IO.Compression.GZipStream($in, [IO.Compression.CompressionMode]::Decompress)
        try {
            $out = [IO.File]::Create($Target)
            try { $gzip.CopyTo($out) } finally { $out.Dispose() }
        } finally { $gzip.Dispose() }
    } finally { $in.Dispose() }
}

# The last non-empty line of a gzip-compressed text file. Reads the whole file, so it also proves it is readable.
function Get-GzipLastLine([string]$Path) {
    $in = [IO.File]::OpenRead($Path)
    try {
        $gzip = New-Object IO.Compression.GZipStream($in, [IO.Compression.CompressionMode]::Decompress)
        $reader = New-Object IO.StreamReader($gzip, [Text.Encoding]::UTF8)
        try {
            $last = ''
            while ($null -ne ($line = $reader.ReadLine())) {
                if ($line.Trim()) { $last = $line }
            }
            return $last
        } finally { $reader.Dispose() }
    } finally { $in.Dispose() }
}

# SHA256SUMS in the format of Linux "sha256sum", so the Docker restore can check Windows backups too
function Write-Checksums([string]$Folder) {
    $lines = @(Get-ChildItem -LiteralPath $Folder -Filter '*.gz' | Sort-Object Name | ForEach-Object {
        '{0}  {1}' -f (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLower(), $_.Name
    })
    Write-Utf8Lines (Join-Path $Folder 'SHA256SUMS') $lines
}

# Files whose checksum doesn't match SHA256SUMS (or that are missing); empty when all is well
function Get-ChecksumProblems([string]$Folder) {
    $sums = Join-Path $Folder 'SHA256SUMS'
    if (-not (Test-Path -LiteralPath $sums)) { return @('SHA256SUMS is missing') }
    $problems = @()
    foreach ($line in [IO.File]::ReadAllLines($sums)) {
        if ($line -match '^([0-9a-f]{64})  (.+)$') {
            $expected = $Matches[1]
            $name = $Matches[2]
            $file = Join-Path $Folder $name
            if (-not (Test-Path -LiteralPath $file)) {
                $problems += "$name is missing"
            } elseif ((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLower() -ne $expected) {
                $problems += "$name is damaged (checksum differs)"
            }
        }
    }
    return $problems
}
