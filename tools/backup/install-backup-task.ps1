<#
.SYNOPSIS
    Registers the nightly HospitalERP backup in Windows Task Scheduler (docs/BACKUPS.md).

.DESCRIPTION
    Runs backup-windows.ps1 every day at -At (default 02:00) as the current user - the user whose MySQL login path
    was stored with mysql_config_editor. If the computer was off or nobody was logged on at that time, the task runs
    as soon as possible afterwards. No administrator rights needed.

.EXAMPLE
    .\install-backup-task.ps1                     # nightly at 02:00 into C:\db-backups\hospital-erp
.EXAMPLE
    .\install-backup-task.ps1 -DryRun             # show what would be registered
.EXAMPLE
    .\install-backup-task.ps1 -Remove             # remove the task again
#>
[CmdletBinding()]
param(
    [string]$At = '02:00',
    [string]$TaskName = 'HospitalERP nightly backup',
    [string]$BackupDir = 'C:\db-backups\hospital-erp',
    [switch]$Remove,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'

if ($Remove) {
    Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
    Write-Host "Removed the task '$TaskName'."
    exit 0
}
if ($At -notmatch '^([01]\d|2[0-3]):[0-5]\d$') {
    Write-Host "-At must be a time like 02:00, not '$At'."
    exit 2
}

$script = Join-Path $PSScriptRoot 'backup-windows.ps1'
$argument = "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$script`" -BackupDir `"$BackupDir`""
$user = [Security.Principal.WindowsIdentity]::GetCurrent().Name

$action = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument $argument -WorkingDirectory $PSScriptRoot
$trigger = New-ScheduledTaskTrigger -Daily -At ([datetime]::ParseExact($At, 'HH:mm', $null))
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit (New-TimeSpan -Hours 2) -MultipleInstances IgnoreNew
$principal = New-ScheduledTaskPrincipal -UserId $user -LogonType Interactive -RunLevel Limited

Write-Host "Task '$TaskName': every day at $At as $user (later if the computer was off),"
Write-Host "  powershell.exe $argument"
if ($DryRun) {
    Write-Host 'Dry run - nothing registered.'
    exit 0
}

Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger -Settings $settings `
    -Principal $principal -Force `
    -Description 'Backs up the HospitalERP database and uploads (docs/BACKUPS.md).' | Out-Null
$next = (Get-ScheduledTaskInfo -TaskName $TaskName).NextRunTime
Write-Host "Registered. Next run: $next. Check $BackupDir\LAST_RUN.txt the morning after."
exit 0
