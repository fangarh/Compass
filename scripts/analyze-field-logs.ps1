param(
    [string]$InputRoot = "artifacts\field-logs",
    [string]$OutputDir = "artifacts\field-analysis",
    [string]$TestStart = "2026-05-19 09:55:30",
    [string]$NearEnd = "2026-05-19 10:00:00",
    [string]$RoomEnd = "2026-05-19 10:01:55",
    [string]$CorridorStart = "2026-05-19 10:02:05",
    [int]$BucketSeconds = 30,
    [switch]$IncludeLegacyRootLogs
)

$ErrorActionPreference = "Stop"

$culture = [System.Globalization.CultureInfo]::InvariantCulture
$timeFormat = "yyyy-MM-dd HH:mm:ss.fff"

function Parse-Marker([string]$value) {
    return [datetime]::ParseExact($value, "yyyy-MM-dd HH:mm:ss", $culture)
}

$testStartTime = Parse-Marker $TestStart
$nearEndTime = Parse-Marker $NearEnd
$roomEndTime = Parse-Marker $RoomEnd
$corridorStartTime = Parse-Marker $CorridorStart

function Get-WindowName([datetime]$time) {
    if ($time -lt $testStartTime) {
        return "pre_test"
    }
    if ($time -lt $nearEndTime) {
        return "near_30cm"
    }
    if ($time -lt $roomEndTime) {
        return "room_5m"
    }
    if ($time -lt $corridorStartTime) {
        return "corridor_transition"
    }
    return "corridor_or_later"
}

function Get-BucketName([datetime]$time) {
    if ($time -lt $testStartTime) {
        return "pre_test"
    }
    $seconds = [int][math]::Floor(($time - $testStartTime).TotalSeconds / $BucketSeconds) * $BucketSeconds
    $start = $testStartTime.AddSeconds($seconds)
    $end = $start.AddSeconds($BucketSeconds)
    return "$($start.ToString('HH:mm:ss'))-$($end.ToString('HH:mm:ss'))"
}

function Get-DeviceName([string]$path) {
    $fullInput = (Resolve-Path $InputRoot).Path
    $fullPath = (Resolve-Path $path).Path
    $relative = $fullPath.Substring($fullInput.Length).TrimStart('\', '/')
    $first = ($relative -split '[\\/]')[0]
    if ([string]::IsNullOrWhiteSpace($first)) {
        return "unknown"
    }
    return $first
}

New-Item -ItemType Directory -Force $OutputDir | Out-Null

$logs = Get-ChildItem -Path $InputRoot -Recurse -Filter "field-radio-*.log" -File
if (-not $IncludeLegacyRootLogs) {
    $logs = $logs | Where-Object {
        $device = Get-DeviceName $_.FullName
        $device -ne "diagnostics"
    }
}

$scanEntries = New-Object System.Collections.Generic.List[object]
$events = New-Object System.Collections.Generic.List[object]

foreach ($log in $logs) {
    $device = Get-DeviceName $log.FullName
    foreach ($line in [System.IO.File]::ReadLines($log.FullName)) {
        if ($line -notmatch '^(?<time>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) .*? WIFI_DIAG (?<message>.*)$') {
            continue
        }

        $time = [datetime]::ParseExact($Matches.time, $timeFormat, $culture)
        $message = $Matches.message
        $window = Get-WindowName $time
        $bucket = Get-BucketName $time

        if ($message -match 'event=scan_entry .*?ssid="(?<ssid>.*?)" bssid=(?<bssid>\S+) level=(?<level>-?\d+) frequency=(?<frequency>\d+) timestamp=(?<scanTimestamp>\d+)') {
            $scanEntries.Add([pscustomobject]@{
                Device = $device
                LogFile = $log.Name
                Time = $time
                Window = $window
                Bucket = $bucket
                Ssid = $Matches.ssid
                Bssid = $Matches.bssid.ToLowerInvariant()
                Rssi = [int]$Matches.level
                Frequency = [int]$Matches.frequency
                ScanTimestamp = [int64]$Matches.scanTimestamp
            })
            continue
        }

        if ($message -match 'event=request accepted=(?<accepted>true|false)') {
            $events.Add([pscustomobject]@{
                Device = $device
                Time = $time
                Window = $window
                Bucket = $bucket
                Event = "request"
                Accepted = $Matches.accepted
                Source = ""
                Updated = ""
                Count = 0
            })
            continue
        }

        if ($message -match 'event=results source=(?<source>\S+) updated=(?<updated>true|false) count=(?<count>\d+)') {
            $events.Add([pscustomobject]@{
                Device = $device
                Time = $time
                Window = $window
                Bucket = $bucket
                Event = "results"
                Accepted = ""
                Source = $Matches.source
                Updated = $Matches.updated
                Count = [int]$Matches.count
            })
        }
    }
}

$scanSummary = $scanEntries |
    Group-Object Window, Device, Bssid |
    ForEach-Object {
        $items = $_.Group
        $rssis = $items | ForEach-Object { $_.Rssi }
        $first = ($items | Sort-Object Time | Select-Object -First 1).Time
        $last = ($items | Sort-Object Time | Select-Object -Last 1).Time
        $ssid = (($items | Where-Object { $_.Ssid } | Group-Object Ssid | Sort-Object Count -Descending | Select-Object -First 1).Name)
        $freq = (($items | Group-Object Frequency | Sort-Object Count -Descending | Select-Object -First 1).Name)
        [pscustomobject]@{
            Window = $items[0].Window
            Device = $items[0].Device
            Bssid = $items[0].Bssid
            Ssid = $ssid
            Frequency = [int]$freq
            Count = $items.Count
            MinRssi = ($rssis | Measure-Object -Minimum).Minimum
            AvgRssi = [math]::Round(($rssis | Measure-Object -Average).Average, 1)
            MaxRssi = ($rssis | Measure-Object -Maximum).Maximum
            FirstSeen = $first.ToString("yyyy-MM-dd HH:mm:ss.fff")
            LastSeen = $last.ToString("yyyy-MM-dd HH:mm:ss.fff")
            SeenSeconds = [math]::Round(($last - $first).TotalSeconds, 1)
        }
    } |
    Sort-Object Window, Device, AvgRssi -Descending

$bucketSummary = $scanEntries |
    Group-Object Bucket, Device, Bssid |
    ForEach-Object {
        $items = $_.Group
        $rssis = $items | ForEach-Object { $_.Rssi }
        $ssid = (($items | Where-Object { $_.Ssid } | Group-Object Ssid | Sort-Object Count -Descending | Select-Object -First 1).Name)
        [pscustomobject]@{
            Bucket = $items[0].Bucket
            Device = $items[0].Device
            Bssid = $items[0].Bssid
            Ssid = $ssid
            Count = $items.Count
            AvgRssi = [math]::Round(($rssis | Measure-Object -Average).Average, 1)
            MinRssi = ($rssis | Measure-Object -Minimum).Minimum
            MaxRssi = ($rssis | Measure-Object -Maximum).Maximum
        }
    } |
    Sort-Object Bucket, Device, AvgRssi -Descending

$eventSummary = $events |
    Group-Object Window, Device |
    ForEach-Object {
        $items = $_.Group
        [pscustomobject]@{
            Window = $items[0].Window
            Device = $items[0].Device
            RequestsAccepted = ($items | Where-Object { $_.Event -eq "request" -and $_.Accepted -eq "true" }).Count
            RequestsRejected = ($items | Where-Object { $_.Event -eq "request" -and $_.Accepted -eq "false" }).Count
            ReceiverUpdated = ($items | Where-Object { $_.Event -eq "results" -and $_.Source -eq "receiver" -and $_.Updated -eq "true" }).Count
            ReceiverCached = ($items | Where-Object { $_.Event -eq "results" -and $_.Source -eq "receiver" -and $_.Updated -eq "false" }).Count
            CachedResultEvents = ($items | Where-Object { $_.Event -eq "results" -and $_.Source -eq "cached" }).Count
        }
    } |
    Sort-Object Window, Device

$windowDeviceSummary = $scanEntries |
    Group-Object Window, Device |
    ForEach-Object {
        $items = $_.Group
        [pscustomobject]@{
            Window = $items[0].Window
            Device = $items[0].Device
            ScanEntries = $items.Count
            UniqueBssid = ($items | Select-Object -ExpandProperty Bssid -Unique).Count
            AvgRssiAll = [math]::Round(($items | Measure-Object Rssi -Average).Average, 1)
            StrongEntries = ($items | Where-Object { $_.Rssi -ge -60 }).Count
            MediumEntries = ($items | Where-Object { $_.Rssi -lt -60 -and $_.Rssi -ge -75 }).Count
            WeakEntries = ($items | Where-Object { $_.Rssi -lt -75 -and $_.Rssi -ge -85 }).Count
            EdgeEntries = ($items | Where-Object { $_.Rssi -lt -85 }).Count
        }
    } |
    Sort-Object Window, Device

$comparison = $scanSummary |
    Group-Object Window, Bssid |
    Where-Object { ($_.Group | Select-Object -ExpandProperty Device -Unique).Count -gt 1 } |
    ForEach-Object {
        $items = $_.Group
        $left = $items | Where-Object Device -eq "R3CT20C8A8N" | Select-Object -First 1
        $right = $items | Where-Object Device -eq "e089985a" | Select-Object -First 1
        if ($left -and $right) {
            [pscustomobject]@{
                Window = $left.Window
                Bssid = $left.Bssid
                Ssid = if ($left.Ssid) { $left.Ssid } else { $right.Ssid }
                SamsungAvgRssi = $left.AvgRssi
                Ne2215AvgRssi = $right.AvgRssi
                DeltaSamsungMinusNe2215 = [math]::Round($left.AvgRssi - $right.AvgRssi, 1)
                SamsungCount = $left.Count
                Ne2215Count = $right.Count
            }
        }
    } |
    Sort-Object Window, Bssid

$scanSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "scan-summary.csv")
$bucketSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "bucket-summary.csv")
$eventSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "event-summary.csv")
$windowDeviceSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "window-device-summary.csv")
$comparison | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "device-comparison.csv")

$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Field Log Analysis")
$report.Add("")
$report.Add("Input: ``$InputRoot``")
$report.Add("")
$report.Add("## Windows")
$report.Add("")
$report.Add("- ``near_30cm``: $TestStart to $NearEnd")
$report.Add("- ``room_5m``: $NearEnd to $RoomEnd")
$report.Add("- ``corridor_transition``: $RoomEnd to $CorridorStart")
$report.Add("- ``corridor_or_later``: from $CorridorStart")
$report.Add("")
$report.Add("## Device Window Summary")
$report.Add("")
$report.Add("| Window | Device | Entries | BSSID | Avg RSSI | Strong | Medium | Weak | Edge |")
$report.Add("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
foreach ($row in $windowDeviceSummary) {
    $report.Add("| $($row.Window) | $($row.Device) | $($row.ScanEntries) | $($row.UniqueBssid) | $($row.AvgRssiAll) | $($row.StrongEntries) | $($row.MediumEntries) | $($row.WeakEntries) | $($row.EdgeEntries) |")
}
$report.Add("")
$report.Add("## Event Summary")
$report.Add("")
$report.Add("| Window | Device | Requests OK | Requests Rejected | Receiver Updated | Receiver Cached | Cached Events |")
$report.Add("| --- | --- | ---: | ---: | ---: | ---: | ---: |")
foreach ($row in $eventSummary) {
    $report.Add("| $($row.Window) | $($row.Device) | $($row.RequestsAccepted) | $($row.RequestsRejected) | $($row.ReceiverUpdated) | $($row.ReceiverCached) | $($row.CachedResultEvents) |")
}
$report.Add("")
$report.Add("## Strongest BSSID Per Window")
$report.Add("")
foreach ($window in ($scanSummary | Select-Object -ExpandProperty Window -Unique)) {
    $report.Add("### $window")
    $report.Add("")
    $report.Add("| Device | BSSID | SSID | Count | Avg RSSI | Min | Max |")
    $report.Add("| --- | --- | --- | ---: | ---: | ---: | ---: |")
    foreach ($row in ($scanSummary | Where-Object Window -eq $window | Sort-Object AvgRssi -Descending | Select-Object -First 12)) {
        $ssid = ($row.Ssid -replace '\|', '/')
        $report.Add("| $($row.Device) | $($row.Bssid) | $ssid | $($row.Count) | $($row.AvgRssi) | $($row.MinRssi) | $($row.MaxRssi) |")
    }
    $report.Add("")
}
$report.Add("## Device Delta For Common BSSID")
$report.Add("")
$report.Add("| Window | BSSID | SSID | Samsung Avg | NE2215 Avg | Delta | Samsung Count | NE2215 Count |")
$report.Add("| --- | --- | --- | ---: | ---: | ---: | ---: | ---: |")
foreach ($row in ($comparison | Sort-Object Window, {[math]::Abs($_.DeltaSamsungMinusNe2215)} -Descending | Select-Object -First 40)) {
    $ssid = ($row.Ssid -replace '\|', '/')
    $report.Add("| $($row.Window) | $($row.Bssid) | $ssid | $($row.SamsungAvgRssi) | $($row.Ne2215AvgRssi) | $($row.DeltaSamsungMinusNe2215) | $($row.SamsungCount) | $($row.Ne2215Count) |")
}

[System.IO.File]::WriteAllLines((Join-Path $OutputDir "summary.md"), $report, [System.Text.UTF8Encoding]::new($false))

Write-Host "Analyzed $($logs.Count) log file(s)."
Write-Host "Scan entries: $($scanEntries.Count)"
Write-Host "Outputs written to $OutputDir"
