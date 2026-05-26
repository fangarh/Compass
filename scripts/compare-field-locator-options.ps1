param(
    [string]$InputRoot = "artifacts\field-mvp-20260523-1836",
    [string]$OutputDir = "artifacts\field-locator-comparison"
)

$ErrorActionPreference = "Stop"
$culture = [System.Globalization.CultureInfo]::InvariantCulture
$timeFormat = "yyyy-MM-dd HH:mm:ss.fff"

function Get-FieldValue([string]$message, [string]$name) {
    $escapedName = [regex]::Escape($name)
    if ($message -match "(^|\s)$escapedName=""(?<quoted>[^""]*)""") {
        return $Matches.quoted
    }
    if ($message -match "(^|\s)$escapedName=(?<plain>\S+)") {
        return $Matches.plain
    }
    return ""
}

function Get-DeviceName([System.IO.FileInfo]$file) {
    $name = [System.IO.Path]::GetFileNameWithoutExtension($file.Name)
    if ($name -match "oneplus") {
        return "oneplus"
    }
    if ($name -match "mi") {
        return "mi"
    }
    return $name
}

function Get-LocatorSource([string]$status) {
    if ([string]::IsNullOrWhiteSpace($status)) {
        return "missing"
    }
    if ($status -match "locator=(?<source>\S+)") {
        return $Matches.source
    }
    return "unparsed"
}

function Get-LocatorDistance([string]$status) {
    if ($status -match "distance=(?<distance>\d+)m") {
        return [int]$Matches.distance
    }
    return $null
}

function Get-TwoAnchorReadiness([string]$status) {
    if ([string]::IsNullOrWhiteSpace($status)) {
        return "MISSING_STATUS"
    }
    $leftReady = $status -match "left=vasya:-\d+"
    $rightReady = $status -match "right=petya:-\d+"
    if ($leftReady -and $rightReady) {
        return "TWO_ANCHORS"
    }
    if ($leftReady -or $rightReady) {
        return "ONE_ANCHOR"
    }
    return "NO_ANCHORS"
}

function Get-WifiTargetDistance([string]$status) {
    if ($status -match "locator=(?<distance>\d+)m") {
        return [int]$Matches.distance
    }
    return $null
}

function Get-WifiTargetClock([string]$status) {
    if ($status -match "clock=(?<clock>\d+|na)") {
        return $Matches.clock
    }
    return ""
}

function New-EventRow([string]$device, [string]$logFile, [datetime]$time, [string]$kind, [string]$source, $distanceM, [string]$message) {
    [pscustomobject]@{
        Device = $device
        LogFile = $logFile
        Time = $time.ToString($timeFormat)
        Kind = $kind
        Source = $source
        DistanceM = if ($null -eq $distanceM) { "" } else { $distanceM }
        LocalPlayerId = Get-FieldValue $message "localDevicePlayerId"
        TargetPlayerId = Get-FieldValue $message "targetPlayerId"
        AnchorPlayerId = Get-FieldValue $message "anchorPlayerId"
        Rssi = Get-FieldValue $message "rssi"
        Sequence = Get-FieldValue $message "sequence"
        GpsStatus = Get-FieldValue $message "gpsStatus"
        GpsAccuracyM = Get-FieldValue $message "gpsAccuracyM"
        GpsLocalLatE7 = Get-FieldValue $message "gpsLocalLatE7"
        GpsLocalLonE7 = Get-FieldValue $message "gpsLocalLonE7"
        GpsLocalAgeMs = Get-FieldValue $message "gpsLocalAgeMs"
        GpsLocalAccuracyM = Get-FieldValue $message "gpsLocalAccuracyM"
        GpsRemoteLatE7 = Get-FieldValue $message "gpsRemoteLatE7"
        GpsRemoteLonE7 = Get-FieldValue $message "gpsRemoteLonE7"
        GpsRemoteAgeMs = Get-FieldValue $message "gpsRemoteAgeMs"
        GpsRemoteAccuracyM = Get-FieldValue $message "gpsRemoteAccuracyM"
        GpsRawDistanceM = Get-FieldValue $message "gpsRawDistanceM"
        GpsRawBearingDeg = Get-FieldValue $message "gpsRawBearingDeg"
        WifiTargetStatus = Get-FieldValue $message "wifiTargetStatus"
        OperatorFieldMapStatus = Get-FieldValue $message "operatorFieldMapStatus"
        TwoAnchorReadiness = Get-TwoAnchorReadiness (Get-FieldValue $message "wifiTargetStatus")
        TwoAnchorDistanceM = Get-WifiTargetDistance (Get-FieldValue $message "wifiTargetStatus")
        TwoAnchorClock = Get-WifiTargetClock (Get-FieldValue $message "wifiTargetStatus")
        Raw = $message
    }
}

New-Item -ItemType Directory -Force $OutputDir | Out-Null

$logs = @(Get-ChildItem -Path $InputRoot -Recurse -Filter "*.log" -File)
if ($logs.Count -eq 0) {
    throw "No log files found under $InputRoot"
}

$events = New-Object System.Collections.Generic.List[object]
$contexts = New-Object System.Collections.Generic.List[object]

foreach ($log in $logs) {
    $device = Get-DeviceName $log
    foreach ($line in [System.IO.File]::ReadLines($log.FullName)) {
        if ($line -notmatch "^(?<time>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}).*? (?<tag>FIELD_DIAG|IFF_DIAG) (?<message>.*)$") {
            continue
        }
        $time = [datetime]::ParseExact($Matches.time, $timeFormat, $culture)
        $tag = $Matches.tag
        $message = $Matches.message
        $event = Get-FieldValue $message "event"

        if ($tag -eq "FIELD_DIAG" -and $event -eq "device_context") {
            $contexts.Add([pscustomobject]@{
                Device = $device
                LogFile = $log.Name
                AppVersionName = Get-FieldValue $message "appVersionName"
                AppVersionCode = Get-FieldValue $message "appVersionCode"
                Model = Get-FieldValue $message "model"
                Sdk = Get-FieldValue $message "sdk"
                BatteryPercent = Get-FieldValue $message "batteryPercent"
            })
            continue
        }

        if ($event -eq "auto_field_check") {
            $locator = Get-FieldValue $message "fieldLocatorStatus"
            $source = Get-LocatorSource $locator
            $distance = Get-LocatorDistance $locator
            $events.Add((New-EventRow $device $log.Name $time "locator_tick" $source $distance $message))
            continue
        }
        if ($event -eq "ble_target_observation") {
            $events.Add((New-EventRow $device $log.Name $time "ble_anchor_observation" "BLE_ANCHOR" $null $message))
            continue
        }
        if ($event -eq "wifi_target_observation") {
            $events.Add((New-EventRow $device $log.Name $time "wifi_ssid_observation" "WIFI_SSID" $null $message))
            continue
        }
        if ($event -eq "wifi_direct_target_observation_rx") {
            $events.Add((New-EventRow $device $log.Name $time "wifi_direct_relay_rx" "WIFI_DIRECT_RELAY" $null $message))
            continue
        }
    }
}

$eventRows = @($events.ToArray())
$eventRows | Export-Csv -Path (Join-Path $OutputDir "locator-events.csv") -NoTypeInformation -Encoding UTF8
$contexts | Export-Csv -Path (Join-Path $OutputDir "device-context.csv") -NoTypeInformation -Encoding UTF8

$sourceSummary = @(
    $eventRows |
        Where-Object Kind -eq "locator_tick" |
        Group-Object Device, Source |
        ForEach-Object {
            $first = $_.Group | Select-Object -First 1
            $distances = @($_.Group | ForEach-Object {
                    if ([string]::IsNullOrWhiteSpace($_.DistanceM)) { $null } else { [double]$_.DistanceM }
                } | Where-Object { $null -ne $_ })
            [pscustomobject]@{
                Device = $first.Device
                Source = $first.Source
                Count = $_.Count
                AvgDistanceM = if ($distances.Count -eq 0) { "" } else { [math]::Round(($distances | Measure-Object -Average).Average, 1) }
                First = ($_.Group | Select-Object -First 1).Time
                Last = ($_.Group | Select-Object -Last 1).Time
            }
        } |
        Sort-Object Device, Source
)
$sourceSummary | Export-Csv -Path (Join-Path $OutputDir "locator-source-summary.csv") -NoTypeInformation -Encoding UTF8

$signalSummary = @(
    $eventRows |
        Where-Object { $_.Kind -ne "locator_tick" } |
        Group-Object Device, Kind, Source |
        ForEach-Object {
            $first = $_.Group | Select-Object -First 1
            $rssis = @($_.Group | ForEach-Object {
                    if ([string]::IsNullOrWhiteSpace($_.Rssi) -or $_.Rssi -eq "127") { $null } else { [double]$_.Rssi }
                } | Where-Object { $null -ne $_ })
            [pscustomobject]@{
                Device = $first.Device
                Kind = $first.Kind
                Source = $first.Source
                Count = $_.Count
                AvgRssi = if ($rssis.Count -eq 0) { "" } else { [math]::Round(($rssis | Measure-Object -Average).Average, 1) }
                MinRssi = if ($rssis.Count -eq 0) { "" } else { ($rssis | Measure-Object -Minimum).Minimum }
                MaxRssi = if ($rssis.Count -eq 0) { "" } else { ($rssis | Measure-Object -Maximum).Maximum }
                First = ($_.Group | Select-Object -First 1).Time
                Last = ($_.Group | Select-Object -Last 1).Time
            }
        } |
        Sort-Object Device, Kind
)
$signalSummary | Export-Csv -Path (Join-Path $OutputDir "signal-source-summary.csv") -NoTypeInformation -Encoding UTF8

$twoAnchorSummary = @(
    $eventRows |
        Where-Object Kind -eq "locator_tick" |
        Group-Object Device, TwoAnchorReadiness, TwoAnchorClock |
        ForEach-Object {
            $first = $_.Group | Select-Object -First 1
            $distances = @($_.Group | ForEach-Object {
                    if ([string]::IsNullOrWhiteSpace($_.TwoAnchorDistanceM)) { $null } else { [double]$_.TwoAnchorDistanceM }
                } | Where-Object { $null -ne $_ })
            [pscustomobject]@{
                Device = $first.Device
                Readiness = $first.TwoAnchorReadiness
                Clock = $first.TwoAnchorClock
                Count = $_.Count
                AvgDistanceM = if ($distances.Count -eq 0) { "" } else { [math]::Round(($distances | Measure-Object -Average).Average, 1) }
                First = ($_.Group | Select-Object -First 1).Time
                Last = ($_.Group | Select-Object -Last 1).Time
            }
        } |
        Sort-Object Device, Readiness, Clock
)
$twoAnchorSummary | Export-Csv -Path (Join-Path $OutputDir "two-anchor-summary.csv") -NoTypeInformation -Encoding UTF8

$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Field Locator Options Comparison")
$report.Add("")
$report.Add("Input: ``$InputRoot``")
$report.Add("")
$report.Add("## Device Context")
$report.Add("")
if ($contexts.Count -eq 0) {
    $report.Add("No device context records found.")
} else {
    $report.Add("| Device | Model | SDK | Battery | App |")
    $report.Add("| --- | --- | ---: | ---: | --- |")
    foreach ($row in $contexts) {
        $report.Add("| $($row.Device) | $($row.Model) | $($row.Sdk) | $($row.BatteryPercent) | $($row.AppVersionName) |")
    }
}
$report.Add("")
$report.Add("## Locator Tick Sources")
$report.Add("")
$report.Add("| Device | Source | Count | Avg distance m | First | Last |")
$report.Add("| --- | --- | ---: | ---: | --- | --- |")
foreach ($row in $sourceSummary) {
    $report.Add("| $($row.Device) | $($row.Source) | $($row.Count) | $($row.AvgDistanceM) | $($row.First) | $($row.Last) |")
}
$report.Add("")
$report.Add("## Signal Sources")
$report.Add("")
$report.Add("| Device | Kind | Source | Count | Avg RSSI | Min | Max | First | Last |")
$report.Add("| --- | --- | --- | ---: | ---: | ---: | ---: | --- | --- |")
foreach ($row in $signalSummary) {
    $report.Add("| $($row.Device) | $($row.Kind) | $($row.Source) | $($row.Count) | $($row.AvgRssi) | $($row.MinRssi) | $($row.MaxRssi) | $($row.First) | $($row.Last) |")
}
$report.Add("")
$report.Add("## Two-Anchor Readiness")
$report.Add("")
$report.Add("| Device | Readiness | Clock | Count | Avg distance m | First | Last |")
$report.Add("| --- | --- | ---: | ---: | ---: | --- | --- |")
foreach ($row in $twoAnchorSummary) {
    $report.Add("| $($row.Device) | $($row.Readiness) | $($row.Clock) | $($row.Count) | $($row.AvgDistanceM) | $($row.First) | $($row.Last) |")
}
$report.Add("")
$report.Add("## Practical Reading")
$report.Add("")
$report.Add("- `WIFI_SSID` means the target phone exposes the expected SSID. This is the cleanest two-anchor radio geometry path, but it requires hotspot/AP visibility.")
$report.Add("- `BLE_ANCHOR` means an anchor phone directly observes the target BLE advert. This works without target Wi-Fi SSID and is the strongest fallback currently observed.")
$report.Add("- `WIFI_DIRECT_RELAY` means one anchor relayed its target RSSI observation through Wi-Fi Direct service discovery. This proves phone-to-phone exchange without a user-created network, but latency is discovery-bound.")
$report.Add("- `GPS_ASSISTED` should only be trusted when GPS status is `GPS_OK`; weak/outlier GPS is diagnostic only.")

[System.IO.File]::WriteAllLines((Join-Path $OutputDir "summary.md"), $report, [System.Text.UTF8Encoding]::new($false))

Write-Host "Compared $($logs.Count) log file(s)."
Write-Host "Locator events: $($eventRows.Count)"
Write-Host "Outputs written to $OutputDir"
