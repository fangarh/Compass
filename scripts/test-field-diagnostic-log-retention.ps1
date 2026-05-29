$ErrorActionPreference = "Stop"

$sourcePath = Join-Path $PSScriptRoot "..\app\src\main\java\net\afterday\compas\logging\FieldDiagnosticLog.java"
$source = Get-Content -LiteralPath $sourcePath -Raw

if ($source -notmatch 'LOG_RETENTION_MS\s*=\s*7L\s*\*\s*24L\s*\*\s*60L\s*\*\s*60L\s*\*\s*1000L') {
    throw "FieldDiagnosticLog must retain field logs for exactly 7 days."
}

if ($source -notmatch 'FILE_PREFIX\s*=\s*"field-radio-"') {
    throw "FieldDiagnosticLog retention must be scoped to field-radio logs."
}

if ($source -notmatch 'FILE_SUFFIX\s*=\s*"\.log"') {
    throw "FieldDiagnosticLog retention must be scoped to .log files."
}

if ($source -notmatch 'RetentionStats\s+retentionStats\s*=\s*deleteOldLogFiles\(dir,\s*System\.currentTimeMillis\(\)\);(?s).*logFile\s*=\s*new File') {
    throw "FieldDiagnosticLog.start must clean old logs before creating the new log file."
}

if ($source -notmatch 'lastModified\s*<\s*cutoffMillis|lastModified\s*>?=\s*cutoffMillis') {
    throw "FieldDiagnosticLog retention must compare file lastModified with the 7-day cutoff."
}

if ($source -notmatch 'file\.delete\(\)') {
    throw "FieldDiagnosticLog retention must delete expired managed log files."
}

if ($source -notmatch 'event=logger_retention') {
    throw "FieldDiagnosticLog must write a retention summary to the new log."
}

Write-Host "Field diagnostic log retention test passed."
