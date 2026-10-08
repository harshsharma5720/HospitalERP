# Stand-in for mysql.exe in test-backup-windows.ps1 (no MySQL on the test machine).
# Logs every call to FAKE_MYSQL_LOG; for "-e source <file>" it copies that file to FAKE_MYSQL_LOADED,
# so the test can check what would have been loaded.
if ($env:FAKE_MYSQL_LOG) { Add-Content -LiteralPath $env:FAKE_MYSQL_LOG -Value ('mysql ' + ($args -join ' ')) }
for ($i = 0; $i -lt $args.Count - 1; $i++) {
    if ($args[$i] -eq '-e' -and $args[$i + 1] -like 'source *') {
        Copy-Item -LiteralPath ($args[$i + 1].Substring('source '.Length)) -Destination $env:FAKE_MYSQL_LOADED
    }
}
exit 0
