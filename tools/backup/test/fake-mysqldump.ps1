# Stand-in for mysqldump.exe in test-backup-windows.ps1 (no MySQL on the test machine).
# Writes a small dump to --result-file. FAKE_DUMP_MODE: "fail" -> "Access denied" on stderr and exit 2;
# "incomplete" -> no "Dump completed" line. Every call is logged to FAKE_MYSQL_LOG.
$out = ''
foreach ($argument in $args) {
    if ($argument -like '--result-file=*') { $out = $argument.Substring('--result-file='.Length) }
}
if ($env:FAKE_MYSQL_LOG) { Add-Content -LiteralPath $env:FAKE_MYSQL_LOG -Value ('mysqldump ' + ($args -join ' ')) }
if ($env:FAKE_DUMP_MODE -eq 'fail') {
    [Console]::Error.WriteLine("mysqldump: Got error: 1045: Access denied for user 'root'@'localhost' (using password: YES)")
    exit 2
}
$lines = @('-- MySQL dump (test stand-in)', "INSERT INTO patient VALUES (1,'Asha'),(2,'Ravi Kumar');")
if ($env:FAKE_DUMP_MODE -ne 'incomplete') { $lines += '-- Dump completed on 2026-10-08  2:00:00' }
[IO.File]::WriteAllLines($out, $lines)
exit 0
