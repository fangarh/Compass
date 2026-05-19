param(
    [string]$InputRoot = "artifacts\field-logs",
    [string]$OutputDir = "artifacts\field-analysis",
    [string]$TestStart = "2026-05-19 09:55:30",
    [string]$NearEnd = "2026-05-19 10:00:00",
    [string]$RoomEnd = "2026-05-19 10:01:55",
    [string]$CorridorStart = "2026-05-19 10:02:05",
    [string]$Windows = "",
    [int]$BucketSeconds = 30,
    [switch]$IncludeLegacyRootLogs
)

$ErrorActionPreference = "Stop"

$culture = [System.Globalization.CultureInfo]::InvariantCulture
$timeFormat = "yyyy-MM-dd HH:mm:ss.fff"

function Parse-Marker([string]$value) {
    return [datetime]::ParseExact($value, "yyyy-MM-dd HH:mm:ss", $culture)
}

function Parse-WindowTime([string]$value, [datetime]$dateHint) {
    $trimmed = $value.Trim()
    if ([string]::IsNullOrWhiteSpace($trimmed)) {
        return $null
    }
    if ($trimmed -match '^\d{2}:\d{2}:\d{2}$') {
        return [datetime]::ParseExact($dateHint.ToString("yyyy-MM-dd") + " " + $trimmed, "yyyy-MM-dd HH:mm:ss", $culture)
    }
    return Parse-Marker $trimmed
}

function New-Window([string]$name, [datetime]$start, [Nullable[datetime]]$end) {
    [pscustomobject]@{
        Name = $name
        Start = $start
        End = $end
    }
}

$testStartTime = Parse-Marker $TestStart
$nearEndTime = Parse-Marker $NearEnd
$roomEndTime = Parse-Marker $RoomEnd
$corridorStartTime = Parse-Marker $CorridorStart
$analysisWindows = New-Object System.Collections.Generic.List[object]

if (-not [string]::IsNullOrWhiteSpace($Windows)) {
    foreach ($spec in ($Windows -split ';')) {
        if ([string]::IsNullOrWhiteSpace($spec)) {
            continue
        }
        $parts = $spec -split '=', 2
        if ($parts.Count -ne 2) {
            throw "Invalid window spec '$spec'. Expected name=start..end."
        }
        $name = $parts[0].Trim()
        $range = $parts[1] -split '\.\.', 2
        if ($range.Count -ne 2) {
            throw "Invalid window range '$($parts[1])'. Expected start..end."
        }
        $start = Parse-WindowTime $range[0] $testStartTime
        $end = Parse-WindowTime $range[1] $testStartTime
        if ($null -eq $start) {
            throw "Window '$name' must have a start time."
        }
        $analysisWindows.Add((New-Window $name $start $end))
    }
    if ($analysisWindows.Count -eq 0) {
        throw "No valid windows were parsed from -Windows."
    }
    $analysisWindows = [System.Collections.Generic.List[object]]($analysisWindows | Sort-Object Start)
    $testStartTime = ($analysisWindows | Select-Object -First 1).Start
} else {
    $analysisWindows.Add((New-Window "near_30cm" $testStartTime $nearEndTime))
    $analysisWindows.Add((New-Window "room_5m" $nearEndTime $roomEndTime))
    $analysisWindows.Add((New-Window "corridor_transition" $roomEndTime $corridorStartTime))
    $analysisWindows.Add((New-Window "corridor_or_later" $corridorStartTime $null))
}

function Get-WindowName([datetime]$time) {
    if ($time -lt $testStartTime) {
        return "pre_test"
    }
    foreach ($window in $analysisWindows) {
        if ($time -ge $window.Start -and ($null -eq $window.End -or $time -lt $window.End)) {
            return $window.Name
        }
    }
    return "post_windows"
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
$contexts = New-Object System.Collections.Generic.List[object]

function Get-FieldValue([string]$message, [string]$name) {
    if ($message -match "$name=""(?<quoted>[^""]*)""") {
        return $Matches.quoted
    }
    if ($message -match "$name=(?<plain>\S+)") {
        return $Matches.plain
    }
    return ""
}

foreach ($log in $logs) {
    $device = Get-DeviceName $log.FullName
    foreach ($line in [System.IO.File]::ReadLines($log.FullName)) {
        if ($line -match '^(?<time>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) .*? FIELD_DIAG (?<message>.*)$') {
            $time = [datetime]::ParseExact($Matches.time, $timeFormat, $culture)
            $message = $Matches.message
            if ($message -match 'event=device_context') {
                $contexts.Add([pscustomobject]@{
                    Device = $device
                    LogFile = $log.Name
                    Time = $time.ToString("yyyy-MM-dd HH:mm:ss.fff")
                    Manufacturer = Get-FieldValue $message "manufacturer"
                    Brand = Get-FieldValue $message "brand"
                    Model = Get-FieldValue $message "model"
                    DeviceName = Get-FieldValue $message "device"
                    Product = Get-FieldValue $message "product"
                    Hardware = Get-FieldValue $message "hardware"
                    Sdk = Get-FieldValue $message "sdk"
                    Release = Get-FieldValue $message "release"
                    AppVersionName = Get-FieldValue $message "appVersionName"
                    AppVersionCode = Get-FieldValue $message "appVersionCode"
                    AndroidIdHash = Get-FieldValue $message "androidIdHash"
                    BatteryPercent = Get-FieldValue $message "batteryPercent"
                    Charging = Get-FieldValue $message "charging"
                    PowerSave = Get-FieldValue $message "powerSave"
                    WifiEnabled = Get-FieldValue $message "wifiEnabled"
                    LocationEnabled = Get-FieldValue $message "locationEnabled"
                })
            }
            continue
        }

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

$deltaPairs = New-Object System.Collections.Generic.List[object]
for ($i = 0; $i -lt $analysisWindows.Count - 1; $i++) {
    $deltaPairs.Add(@($analysisWindows[$i].Name, $analysisWindows[$i + 1].Name))
}
if ($analysisWindows.Count -gt 2) {
    $deltaPairs.Add(@($analysisWindows[0].Name, $analysisWindows[$analysisWindows.Count - 1].Name))
}

$movementDeltas = New-Object System.Collections.Generic.List[object]
foreach ($device in ($scanSummary | Select-Object -ExpandProperty Device -Unique)) {
    foreach ($bssid in ($scanSummary | Where-Object Device -eq $device | Select-Object -ExpandProperty Bssid -Unique)) {
        $rows = $scanSummary | Where-Object { $_.Device -eq $device -and $_.Bssid -eq $bssid }
        foreach ($pair in $deltaPairs) {
            $from = $rows | Where-Object Window -eq $pair[0] | Select-Object -First 1
            $to = $rows | Where-Object Window -eq $pair[1] | Select-Object -First 1
            if ($from -and $to) {
                $delta = [math]::Round([double]$to.AvgRssi - [double]$from.AvgRssi, 1)
                $movementDeltas.Add([pscustomobject]@{
                    Device = $device
                    Bssid = $bssid
                    Ssid = if ($from.Ssid) { $from.Ssid } else { $to.Ssid }
                    FromWindow = $pair[0]
                    ToWindow = $pair[1]
                    FromAvgRssi = $from.AvgRssi
                    ToAvgRssi = $to.AvgRssi
                    DeltaRssi = $delta
                    AbsDeltaRssi = [math]::Abs($delta)
                    FromCount = $from.Count
                    ToCount = $to.Count
                    Candidate = ([math]::Abs($delta) -ge 8 -and [int]$from.Count -ge 3 -and [int]$to.Count -ge 3)
                })
            }
        }
    }
}

$movementDeltas = $movementDeltas | Sort-Object `
    @{ Expression = "Candidate"; Descending = $true },
    @{ Expression = "AbsDeltaRssi"; Descending = $true },
    Device,
    Bssid

$fingerprints = $scanSummary |
    Where-Object { $_.Window -ne "pre_test" -and $_.Window -ne "post_windows" -and [int]$_.Count -ge 3 } |
    ForEach-Object {
        [pscustomobject]@{
            Device = $_.Device
            Zone = $_.Window
            Bssid = $_.Bssid
            Ssid = $_.Ssid
            AvgRssi = [double]$_.AvgRssi
            Count = [int]$_.Count
        }
    } |
    Sort-Object Device, Zone, Bssid

$zoneEvaluation = New-Object System.Collections.Generic.List[object]
$bucketGroups = $bucketSummary |
    Where-Object { $_.Bucket -ne "pre_test" } |
    Group-Object Bucket, Device

foreach ($bucketGroup in $bucketGroups) {
    $bucketItems = $bucketGroup.Group
    $device = $bucketItems[0].Device
    $actualWindow = (($scanEntries | Where-Object { $_.Device -eq $device -and $_.Bucket -eq $bucketItems[0].Bucket } | Select-Object -First 1).Window)
    $zones = $fingerprints | Where-Object Device -eq $device | Select-Object -ExpandProperty Zone -Unique
    foreach ($zone in $zones) {
        $zoneFingerprint = $fingerprints | Where-Object { $_.Device -eq $device -and $_.Zone -eq $zone }
        $shared = New-Object System.Collections.Generic.List[object]
        foreach ($bucketItem in $bucketItems) {
            $fingerprintItem = $zoneFingerprint | Where-Object Bssid -eq $bucketItem.Bssid | Select-Object -First 1
            if ($fingerprintItem) {
                $shared.Add([pscustomobject]@{
                    Bssid = $bucketItem.Bssid
                    BucketAvgRssi = [double]$bucketItem.AvgRssi
                    FingerprintAvgRssi = [double]$fingerprintItem.AvgRssi
                    AbsError = [math]::Abs([double]$bucketItem.AvgRssi - [double]$fingerprintItem.AvgRssi)
                })
            }
        }

        $sharedCount = $shared.Count
        $bucketBssidCount = ($bucketItems | Select-Object -ExpandProperty Bssid -Unique).Count
        $fingerprintBssidCount = ($zoneFingerprint | Select-Object -ExpandProperty Bssid -Unique).Count
        $avgAbsError = if ($sharedCount -gt 0) { [math]::Round(($shared | Measure-Object AbsError -Average).Average, 2) } else { 99.0 }
        $overlapBucket = if ($bucketBssidCount -gt 0) { [math]::Round($sharedCount / $bucketBssidCount, 3) } else { 0 }
        $overlapFingerprint = if ($fingerprintBssidCount -gt 0) { [math]::Round($sharedCount / $fingerprintBssidCount, 3) } else { 0 }
        $score = [math]::Round(($sharedCount * 2.0) + ($overlapBucket * 20.0) + ($overlapFingerprint * 10.0) - $avgAbsError, 3)

        $zoneEvaluation.Add([pscustomobject]@{
            Bucket = $bucketItems[0].Bucket
            Device = $device
            ActualWindow = $actualWindow
            CandidateZone = $zone
            Score = $score
            SharedBssid = $sharedCount
            BucketBssid = $bucketBssidCount
            FingerprintBssid = $fingerprintBssidCount
            OverlapBucket = $overlapBucket
            OverlapFingerprint = $overlapFingerprint
            AvgAbsRssiError = $avgAbsError
        })
    }
}

$zoneEvaluation = $zoneEvaluation | Sort-Object Bucket, Device, Score -Descending
$zonePredictions = $zoneEvaluation |
    Group-Object Bucket, Device |
    ForEach-Object {
        $best = $_.Group | Sort-Object Score -Descending | Select-Object -First 1
        [pscustomobject]@{
            Bucket = $best.Bucket
            Device = $best.Device
            ActualWindow = $best.ActualWindow
            PredictedZone = $best.CandidateZone
            Correct = ($best.ActualWindow -eq $best.CandidateZone)
            Score = $best.Score
            SharedBssid = $best.SharedBssid
            BucketBssid = $best.BucketBssid
            FingerprintBssid = $best.FingerprintBssid
            OverlapBucket = $best.OverlapBucket
            AvgAbsRssiError = $best.AvgAbsRssiError
        }
    } |
    Sort-Object Bucket, Device

$scanSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "scan-summary.csv")
$bucketSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "bucket-summary.csv")
$eventSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "event-summary.csv")
$windowDeviceSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "window-device-summary.csv")
$comparison | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "device-comparison.csv")
$contexts | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "device-context.csv")
$movementDeltas | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "movement-deltas.csv")
$fingerprints | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "zone-fingerprints.csv")
$zoneEvaluation | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "zone-evaluation.csv")
$zonePredictions | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "zone-predictions.csv")

$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Field Log Analysis")
$report.Add("")
$report.Add("Input: ``$InputRoot``")
$report.Add("")
$report.Add("## Windows")
$report.Add("")
foreach ($window in $analysisWindows) {
    $endText = if ($null -eq $window.End) { "open" } else { $window.End.ToString("yyyy-MM-dd HH:mm:ss") }
    $report.Add("- ``$($window.Name)``: $($window.Start.ToString('yyyy-MM-dd HH:mm:ss')) to $endText")
}
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
$report.Add("## Device Context")
$report.Add("")
if ($contexts.Count -eq 0) {
    $report.Add("No `FIELD_DIAG event=device_context` lines found. Rebuild and rerun the app to collect Phase 2 context headers.")
} else {
    $report.Add("| Device | Model | SDK | Battery | Charging | Power Save | Wi-Fi | Location | App |")
    $report.Add("| --- | --- | ---: | ---: | --- | --- | --- | --- | --- |")
    foreach ($row in $contexts) {
        $report.Add("| $($row.Device) | $($row.Manufacturer) $($row.Model) | $($row.Sdk) | $($row.BatteryPercent) | $($row.Charging) | $($row.PowerSave) | $($row.WifiEnabled) | $($row.LocationEnabled) | $($row.AppVersionName) |")
    }
}
$report.Add("")
$report.Add("## Movement Candidates")
$report.Add("")
$report.Add("| Device | BSSID | SSID | From | To | From Avg | To Avg | Delta | Counts |")
$report.Add("| --- | --- | --- | --- | --- | ---: | ---: | ---: | --- |")
foreach ($row in ($movementDeltas | Where-Object Candidate -eq $true | Select-Object -First 30)) {
    $ssid = ($row.Ssid -replace '\|', '/')
    $report.Add("| $($row.Device) | $($row.Bssid) | $ssid | $($row.FromWindow) | $($row.ToWindow) | $($row.FromAvgRssi) | $($row.ToAvgRssi) | $($row.DeltaRssi) | $($row.FromCount)/$($row.ToCount) |")
}
$report.Add("")
$report.Add("## Zone Fingerprint Evaluation")
$report.Add("")
if ($zonePredictions.Count -eq 0) {
    $report.Add("No zone predictions generated.")
} else {
    $correct = ($zonePredictions | Where-Object Correct -eq $true).Count
    $total = $zonePredictions.Count
    $accuracy = [math]::Round(($correct * 100.0) / $total, 1)
    $report.Add("Best-zone bucket accuracy against named windows: $correct/$total ($accuracy%).")
    $report.Add("")
    $report.Add("| Bucket | Device | Actual | Predicted | Correct | Shared | Error | Score |")
    $report.Add("| --- | --- | --- | --- | --- | ---: | ---: | ---: |")
    foreach ($row in $zonePredictions) {
        $report.Add("| $($row.Bucket) | $($row.Device) | $($row.ActualWindow) | $($row.PredictedZone) | $($row.Correct) | $($row.SharedBssid) | $($row.AvgAbsRssiError) | $($row.Score) |")
    }
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
