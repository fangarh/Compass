param(
    [string]$InputRoot = "artifacts\field-logs",
    [Alias("OutputRoot")]
    [string]$OutputDir = "artifacts\field-analysis",
    [string]$TestStart = "2026-05-19 09:55:30",
    [string]$NearEnd = "2026-05-19 10:00:00",
    [string]$RoomEnd = "2026-05-19 10:01:55",
    [string]$CorridorStart = "2026-05-19 10:02:05",
    [string]$Windows = "",
    [int]$BucketSeconds = 30,
    [double]$OfficeBaselineSideA = [double]::NaN,
    [double]$OfficeBaselineSideB = [double]::NaN,
    [string]$BeaconSsids = "COMPASS_BEACON*",
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
        $device -ne "diagnostics" -and $device -notlike "field-radio-*.log"
    }
}

$scanEntries = New-Object System.Collections.Generic.List[object]
$beaconEntries = New-Object System.Collections.Generic.List[object]
$events = New-Object System.Collections.Generic.List[object]
$contexts = New-Object System.Collections.Generic.List[object]
$freshnessTicks = New-Object System.Collections.Generic.List[object]
$sensorEvents = New-Object System.Collections.Generic.List[object]
$locationEvents = New-Object System.Collections.Generic.List[object]
$iffFieldChecks = New-Object System.Collections.Generic.List[object]
$bleRssiEvents = New-Object System.Collections.Generic.List[object]

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

function Get-NumberOrNull([string]$value) {
    if ([string]::IsNullOrWhiteSpace($value) -or $value -eq "na") {
        return $null
    }
    return [double]::Parse($value, $culture)
}

$beaconSsidPatterns = @(
    $BeaconSsids -split ',' |
        ForEach-Object { $_.Trim() } |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
)

function Test-BeaconSsid([string]$ssid) {
    if ([string]::IsNullOrWhiteSpace($ssid) -or $beaconSsidPatterns.Count -eq 0) {
        return $false
    }
    foreach ($pattern in $beaconSsidPatterns) {
        if ($ssid -like $pattern) {
            return $true
        }
    }
    return $false
}

function Get-BeaconRangeClass([int]$rssi) {
    if ($rssi -ge -45) {
        return "very_close"
    }
    if ($rssi -ge -55) {
        return "close"
    }
    if ($rssi -ge -67) {
        return "medium"
    }
    if ($rssi -ge -80) {
        return "far"
    }
    return "edge"
}

function Get-BeaconTrend([Nullable[double]]$deltaDb) {
    if ($null -eq $deltaDb) {
        return "first"
    }
    if ($deltaDb -ge 3.0) {
        return "stronger"
    }
    if ($deltaDb -le -3.0) {
        return "weaker"
    }
    return "stable"
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

        if ($line -match '^(?<time>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) .*? SENSOR_DIAG (?<message>.*)$') {
            $time = [datetime]::ParseExact($Matches.time, $timeFormat, $culture)
            $message = $Matches.message
            $window = Get-WindowName $time
            $bucket = Get-BucketName $time
            $sensorEvents.Add([pscustomobject]@{
                Device = $device
                LogFile = $log.Name
                Time = $time
                Window = $window
                Bucket = $bucket
                Event = Get-FieldValue $message "event"
                Name = Get-FieldValue $message "name"
                IntervalMs = Get-FieldValue $message "intervalMs"
                Accel = Get-FieldValue $message "accel"
                Gyro = Get-FieldValue $message "gyro"
                Magnetic = Get-FieldValue $message "magnetic"
                YawDeg = Get-FieldValue $message "yawDeg"
                PitchDeg = Get-FieldValue $message "pitchDeg"
                RollDeg = Get-FieldValue $message "rollDeg"
                PressureHpa = Get-FieldValue $message "pressureHpa"
                LightLux = Get-FieldValue $message "lightLux"
                ProximityCm = Get-FieldValue $message "proximityCm"
                StepCounter = Get-FieldValue $message "stepCounter"
                LocationAgeMs = Get-FieldValue $message "locationAgeMs"
                Registered = Get-FieldValue $message "registered"
                Available = Get-FieldValue $message "available"
                Message = $message
            })
            continue
        }

        if ($line -match '^(?<time>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) .*? LOCATION_DIAG (?<message>.*)$') {
            $time = [datetime]::ParseExact($Matches.time, $timeFormat, $culture)
            $message = $Matches.message
            $window = Get-WindowName $time
            $bucket = Get-BucketName $time
            $locationEvents.Add([pscustomobject]@{
                Device = $device
                LogFile = $log.Name
                Time = $time
                Window = $window
                Bucket = $bucket
                Event = Get-FieldValue $message "event"
                Provider = Get-FieldValue $message "provider"
                Lat = Get-FieldValue $message "lat"
                Lon = Get-FieldValue $message "lon"
                AccuracyM = Get-FieldValue $message "accuracyM"
                AltitudeM = Get-FieldValue $message "altitudeM"
                SpeedMps = Get-FieldValue $message "speedMps"
                BearingDeg = Get-FieldValue $message "bearingDeg"
                Satellites = Get-FieldValue $message "satellites"
                LocationTimeMs = Get-FieldValue $message "timeMs"
                Message = $message
            })
            continue
        }

        if ($line -match '^(?<time>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) .*? IFF_DIAG (?<message>.*)$') {
            $time = [datetime]::ParseExact($Matches.time, $timeFormat, $culture)
            $message = $Matches.message
            $event = Get-FieldValue $message "event"
            if ($event -eq "ble_field_radio_rx") {
                $window = Get-WindowName $time
                $bucket = Get-BucketName $time
                $rssi = Get-NumberOrNull (Get-FieldValue $message "rssi")
                $bleRssiEvents.Add([pscustomobject]@{
                    Device = $device
                    LogFile = $log.Name
                    Time = $time
                    Window = $window
                    Bucket = $bucket
                    LocalPlayerId = Get-FieldValue $message "localPlayerId"
                    SeenPlayerId = Get-FieldValue $message "playerId"
                    Address = Get-FieldValue $message "address"
                    Rssi = $rssi
                    IsOutlier127 = ($rssi -eq 127)
                    Contract = Get-FieldValue $message "contract"
                    Message = $message
                })
            }
            if ($event -eq "field_check" -or $event -eq "auto_field_check") {
                $window = Get-WindowName $time
                $bucket = Get-BucketName $time
                $iffFieldChecks.Add([pscustomobject]@{
                    Source = if ($event -eq "auto_field_check") { "auto" } else { "manual" }
                    SnapshotSource = if ($event -eq "auto_field_check") { Get-FieldValue $message "source" } else { "" }
                    Device = $device
                    LogFile = $log.Name
                    Time = $time
                    Window = $window
                    Bucket = $bucket
                    PlayerId = Get-FieldValue $message "playerId"
                    DisplayName = Get-FieldValue $message "displayName"
                    LocalDevicePlayerId = Get-FieldValue $message "localDevicePlayerId"
                    OfficeRole = Get-FieldValue $message "officeRole"
                    SelectedOfficeRole = Get-FieldValue $message "selectedOfficeRole"
                    SelectedIsLocalDevice = Get-FieldValue $message "selectedIsLocalDevice"
                    TrustedPlayer = Get-FieldValue $message "trustedPlayer"
                    TrustLabel = Get-FieldValue $message "trustLabel"
                    CombatState = Get-FieldValue $message "combatState"
                    CombatAction = Get-FieldValue $message "combatAction"
                    IdentityLabel = Get-FieldValue $message "identityLabel"
                    IdentityScore = Get-NumberOrNull (Get-FieldValue $message "identityScore")
                    ProximityLabel = Get-FieldValue $message "proximityLabel"
                    ProximityScore = Get-NumberOrNull (Get-FieldValue $message "proximityScore")
                    PositionLabel = Get-FieldValue $message "positionLabel"
                    PositionScore = Get-NumberOrNull (Get-FieldValue $message "positionScore")
                    DirectionLabel = Get-FieldValue $message "directionLabel"
                    DirectionScore = Get-NumberOrNull (Get-FieldValue $message "directionScore")
                    OperatorVerdict = Get-FieldValue $message "operatorVerdict"
                    OfficeProximityVerdict = Get-FieldValue $message "officeProximityVerdict"
                    OfficeProximityDeltaDb = Get-NumberOrNull (Get-FieldValue $message "officeProximityDeltaDb")
                    OfficeProximityReason = Get-FieldValue $message "officeProximityReason"
                    OfficeProximityA = Get-FieldValue $message "officeProximityA"
                    OfficeProximityB = Get-FieldValue $message "officeProximityB"
                    DistanceTargetPlayerId = Get-FieldValue $message "distanceTargetPlayerId"
                    DistanceClass = Get-FieldValue $message "distanceClass"
                    DistanceConfidence = Get-NumberOrNull (Get-FieldValue $message "distanceConfidence")
                    MovementTrend = Get-FieldValue $message "movementTrend"
                    MovementConfidence = Get-NumberOrNull (Get-FieldValue $message "movementConfidence")
                    MovementRssiDeltaDb = Get-NumberOrNull (Get-FieldValue $message "movementRssiDeltaDb")
                    GpsStatus = Get-FieldValue $message "gpsStatus"
                    GpsAccuracyM = Get-NumberOrNull (Get-FieldValue $message "gpsAccuracyM")
                    GpsDistanceM = Get-NumberOrNull (Get-FieldValue $message "gpsDistanceM")
                    GpsBearingDeg = Get-NumberOrNull (Get-FieldValue $message "gpsBearingDeg")
                    WitnessQuorum = Get-FieldValue $message "witnessQuorum"
                    WitnessFreshSources = Get-NumberOrNull (Get-FieldValue $message "witnessFreshSources")
                    WitnessPossibleSources = Get-NumberOrNull (Get-FieldValue $message "witnessPossibleSources")
                    RemoteWitnessContract = Get-FieldValue $message "remoteWitnessContract"
                    RemoteReportCount = Get-NumberOrNull (Get-FieldValue $message "remoteReportCount")
                    RemoteFreshSources = Get-NumberOrNull (Get-FieldValue $message "remoteFreshSources")
                    RemoteStaleSources = Get-NumberOrNull (Get-FieldValue $message "remoteStaleSources")
                    FieldRadioStatus = Get-FieldValue $message "fieldRadioStatus"
                    FieldRadioPolicy = Get-FieldValue $message "fieldRadioPolicy"
                    FieldRadioEnabled = Get-FieldValue $message "fieldRadioEnabled"
                    WifiFingerprintStatus = Get-FieldValue $message "wifiFingerprintStatus"
                    WifiRefreshRequested = Get-FieldValue $message "wifiRefreshRequested"
                    WifiRefreshAccepted = Get-FieldValue $message "wifiRefreshAccepted"
                    WifiFreshness = Get-FieldValue $message "wifiFreshness"
                    WifiFreshAgeMs = Get-NumberOrNull (Get-FieldValue $message "wifiFreshAgeMs")
                    WifiFingerprint = Get-FieldValue $message "wifiFingerprint"
                    TransportStatus = Get-FieldValue $message "transportStatus"
                    WitnessFreshness = if ($event -eq "auto_field_check") { "" } else { Get-FieldValue $message "witness" }
                    WitnessRssi = if ($event -eq "auto_field_check") { $null } else { Get-NumberOrNull (Get-FieldValue $message "rssi") }
                    WitnessAgeMs = if ($event -eq "auto_field_check") { $null } else { Get-NumberOrNull (Get-FieldValue $message "ageMs") }
                    WitnessSsid = if ($event -eq "auto_field_check") { "" } else { Get-FieldValue $message "ssid" }
                    WitnessBssid = if ($event -eq "auto_field_check") { "" } else { Get-FieldValue $message "bssid" }
                    LocalApproach = if ($event -eq "auto_field_check") { "" } else { Get-FieldValue $message "localApproach" }
                    Message = $message
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

        if ($message -match 'event=tick .*?freshAgeMs=(?<freshAge>-?\d+) cachedCount=(?<cachedCount>-?\d+)') {
            $freshnessTicks.Add([pscustomobject]@{
                Device = $device
                Time = $time
                Window = $window
                Bucket = $bucket
                FreshAgeMs = [int64]$Matches.freshAge
                CachedCount = [int]$Matches.cachedCount
            })
            continue
        }

        if ($message -match 'event=scan_entry .*?ssid="(?<ssid>.*?)" bssid=(?<bssid>\S+) level=(?<level>-?\d+) frequency=(?<frequency>\d+) timestamp=(?<scanTimestamp>\d+)') {
            $entry = [pscustomobject]@{
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
            }
            $scanEntries.Add($entry)
            if (Test-BeaconSsid $entry.Ssid) {
                $beaconEntries.Add([pscustomobject]@{
                    Device = $entry.Device
                    LogFile = $entry.LogFile
                    Time = $entry.Time
                    Window = $entry.Window
                    Bucket = $entry.Bucket
                    Ssid = $entry.Ssid
                    Bssid = $entry.Bssid
                    Rssi = $entry.Rssi
                    Frequency = $entry.Frequency
                    ScanTimestamp = $entry.ScanTimestamp
                    RangeClass = Get-BeaconRangeClass $entry.Rssi
                })
            }
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

$beaconTimeline = $beaconEntries |
    Sort-Object Time, Device, Ssid, Bssid |
    ForEach-Object {
        [pscustomobject]@{
            Device = $_.Device
            LogFile = $_.LogFile
            Time = $_.Time.ToString("yyyy-MM-dd HH:mm:ss.fff")
            Window = $_.Window
            Bucket = $_.Bucket
            Ssid = $_.Ssid
            Bssid = $_.Bssid
            Rssi = $_.Rssi
            Frequency = $_.Frequency
            RangeClass = $_.RangeClass
            ScanTimestamp = $_.ScanTimestamp
        }
    }

$beaconBucketRaw = @(
    $beaconEntries |
        Group-Object Window, Device, Bucket, Ssid, Bssid |
        ForEach-Object {
            $items = $_.Group
            $rssis = $items | ForEach-Object { $_.Rssi }
            $first = ($items | Sort-Object Time | Select-Object -First 1).Time
            $last = ($items | Sort-Object Time | Select-Object -Last 1).Time
            $freq = (($items | Group-Object Frequency | Sort-Object Count -Descending | Select-Object -First 1).Name)
            $avg = [math]::Round(($rssis | Measure-Object -Average).Average, 1)
            [pscustomobject]@{
                Window = $items[0].Window
                Device = $items[0].Device
                Bucket = $items[0].Bucket
                Ssid = $items[0].Ssid
                Bssid = $items[0].Bssid
                Frequency = [int]$freq
                Count = $items.Count
                AvgRssi = $avg
                MinRssi = ($rssis | Measure-Object -Minimum).Minimum
                MaxRssi = ($rssis | Measure-Object -Maximum).Maximum
                RangeClass = Get-BeaconRangeClass ([int][math]::Round($avg, 0))
                FirstSeenTime = $first
                LastSeenTime = $last
            }
        }
)

$beaconBucketSummary = New-Object System.Collections.Generic.List[object]
$lastBeaconBucketByKey = @{}
$secondLastBeaconBucketByKey = @{}
foreach ($row in ($beaconBucketRaw | Sort-Object Device, Ssid, Bssid, FirstSeenTime)) {
    $key = "$($row.Device)|$($row.Ssid)|$($row.Bssid)"
    $previous = $lastBeaconBucketByKey[$key]
    $secondPrevious = $secondLastBeaconBucketByKey[$key]
    $trendDb = $null
    $trend10sDb = $null
    if ($null -ne $previous) {
        $trendDb = [math]::Round($row.AvgRssi - $previous.AvgRssi, 1)
    }
    if ($null -ne $secondPrevious) {
        $trend10sDb = [math]::Round($row.AvgRssi - $secondPrevious.AvgRssi, 1)
    }
    $beaconBucketSummary.Add([pscustomobject]@{
        Window = $row.Window
        Device = $row.Device
        Bucket = $row.Bucket
        Ssid = $row.Ssid
        Bssid = $row.Bssid
        Frequency = $row.Frequency
        Count = $row.Count
        AvgRssi = $row.AvgRssi
        MinRssi = $row.MinRssi
        MaxRssi = $row.MaxRssi
        RangeClass = $row.RangeClass
        TrendDb = $trendDb
        Trend10sDb = $trend10sDb
        Trend = Get-BeaconTrend $trendDb
        FirstSeen = $row.FirstSeenTime.ToString("yyyy-MM-dd HH:mm:ss.fff")
        LastSeen = $row.LastSeenTime.ToString("yyyy-MM-dd HH:mm:ss.fff")
    })
    $secondLastBeaconBucketByKey[$key] = $previous
    $lastBeaconBucketByKey[$key] = $row
}
$beaconBucketSummary = $beaconBucketSummary | Sort-Object FirstSeen, Device, Ssid, Bssid

$beaconSummary = $beaconEntries |
    Group-Object Window, Device, Ssid, Bssid |
    ForEach-Object {
        $items = $_.Group
        $rssis = $items | ForEach-Object { $_.Rssi }
        $first = ($items | Sort-Object Time | Select-Object -First 1).Time
        $last = ($items | Sort-Object Time | Select-Object -Last 1).Time
        [pscustomobject]@{
            Window = $items[0].Window
            Device = $items[0].Device
            Ssid = $items[0].Ssid
            Bssid = $items[0].Bssid
            Count = $items.Count
            AvgRssi = [math]::Round(($rssis | Measure-Object -Average).Average, 1)
            MinRssi = ($rssis | Measure-Object -Minimum).Minimum
            MaxRssi = ($rssis | Measure-Object -Maximum).Maximum
            VeryClose = @($items | Where-Object RangeClass -eq "very_close").Count
            Close = @($items | Where-Object RangeClass -eq "close").Count
            Medium = @($items | Where-Object RangeClass -eq "medium").Count
            Far = @($items | Where-Object RangeClass -eq "far").Count
            Edge = @($items | Where-Object RangeClass -eq "edge").Count
            FirstSeen = $first.ToString("yyyy-MM-dd HH:mm:ss.fff")
            LastSeen = $last.ToString("yyyy-MM-dd HH:mm:ss.fff")
            SeenSeconds = [math]::Round(($last - $first).TotalSeconds, 1)
        }
    } |
    Sort-Object Window, Device, AvgRssi -Descending

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

$freshnessSummary = $freshnessTicks |
    Group-Object Window, Device |
    ForEach-Object {
        $items = $_.Group
        $validFresh = $items | Where-Object { $_.FreshAgeMs -ge 0 }
        $receiverUpdates = $events | Where-Object {
            $_.Window -eq $items[0].Window -and
            $_.Device -eq $items[0].Device -and
            $_.Event -eq "results" -and
            $_.Source -eq "receiver" -and
            $_.Updated -eq "true"
        } | Sort-Object Time
        $gaps = New-Object System.Collections.Generic.List[double]
        for ($i = 1; $i -lt $receiverUpdates.Count; $i++) {
            $gaps.Add(($receiverUpdates[$i].Time - $receiverUpdates[$i - 1].Time).TotalSeconds)
        }
        [pscustomobject]@{
            Window = $items[0].Window
            Device = $items[0].Device
            TickCount = $items.Count
            ReceiverUpdated = $receiverUpdates.Count
            AvgFreshAgeMs = if ($validFresh.Count -gt 0) { [math]::Round(($validFresh | Measure-Object FreshAgeMs -Average).Average, 0) } else { -1 }
            MaxFreshAgeMs = if ($validFresh.Count -gt 0) { ($validFresh | Measure-Object FreshAgeMs -Maximum).Maximum } else { -1 }
            StaleOver3000 = ($validFresh | Where-Object { $_.FreshAgeMs -gt 3000 }).Count
            StaleOver5000 = ($validFresh | Where-Object { $_.FreshAgeMs -gt 5000 }).Count
            NoFreshYet = ($items | Where-Object { $_.FreshAgeMs -lt 0 }).Count
            AvgCachedCount = [math]::Round(($items | Measure-Object CachedCount -Average).Average, 1)
            AvgReceiverGapSec = if ($gaps.Count -gt 0) { [math]::Round(($gaps | Measure-Object -Average).Average, 2) } else { -1 }
            MaxReceiverGapSec = if ($gaps.Count -gt 0) { [math]::Round(($gaps | Measure-Object -Maximum).Maximum, 2) } else { -1 }
        }
    } |
    Sort-Object Window, Device

$sensorSummary = $sensorEvents |
    Group-Object Window, Device |
    ForEach-Object {
        $items = $_.Group
        $ticks = $items | Where-Object Event -eq "tick"
        [pscustomobject]@{
            Window = $items[0].Window
            Device = $items[0].Device
            TickCount = $ticks.Count
            SensorEvents = $items.Count
            RegisteredSensors = ($items | Where-Object { $_.Event -eq "sensor_register" -and $_.Registered -eq "true" }).Count
            UnavailableSensors = ($items | Where-Object { $_.Event -eq "sensor_register" -and $_.Available -eq "false" }).Count
            YawSamples = ($ticks | Where-Object { $_.YawDeg -ne "" -and $_.YawDeg -ne "na" }).Count
            PressureSamples = ($ticks | Where-Object { $_.PressureHpa -ne "" -and $_.PressureHpa -ne "na" }).Count
            LightSamples = ($ticks | Where-Object { $_.LightLux -ne "" -and $_.LightLux -ne "na" }).Count
            StepSamples = ($ticks | Where-Object { $_.StepCounter -ne "" -and $_.StepCounter -ne "na" }).Count
        }
    } |
    Sort-Object Window, Device

$locationSummary = $locationEvents |
    Group-Object Window, Device |
    ForEach-Object {
        $items = $_.Group
        $updates = $items | Where-Object { $_.Event -eq "update" -or $_.Event -eq "last_known" }
        $accuracies = $updates |
            ForEach-Object { Get-NumberOrNull $_.AccuracyM } |
            Where-Object { $null -ne $_ }
        [pscustomobject]@{
            Window = $items[0].Window
            Device = $items[0].Device
            LocationEvents = $items.Count
            Updates = ($items | Where-Object Event -eq "update").Count
            LastKnown = ($items | Where-Object Event -eq "last_known").Count
            GpsUpdates = ($items | Where-Object { $_.Event -eq "update" -and $_.Provider -eq "gps" }).Count
            NetworkUpdates = ($items | Where-Object { $_.Event -eq "update" -and $_.Provider -eq "network" }).Count
            BestAccuracyM = if ($accuracies.Count -gt 0) { [math]::Round(($accuracies | Measure-Object -Minimum).Minimum, 1) } else { -1 }
            AvgAccuracyM = if ($accuracies.Count -gt 0) { [math]::Round(($accuracies | Measure-Object -Average).Average, 1) } else { -1 }
        }
    } |
    Sort-Object Window, Device

function Test-ValidCoordinate([Nullable[double]]$lat, [Nullable[double]]$lon) {
    return $null -ne $lat -and
        $null -ne $lon -and
        $lat -ge -90.0 -and
        $lat -le 90.0 -and
        $lon -ge -180.0 -and
        $lon -le 180.0
}

function Get-GpsPairStatus($from, $to) {
    if ($null -eq $from -or $null -eq $to) {
        return "GPS_UNAVAILABLE"
    }
    if ($null -eq $from.AccuracyM -or $null -eq $to.AccuracyM) {
        return "GPS_WEAK"
    }
    if ([double]$from.AccuracyM -le 25.0 -and [double]$to.AccuracyM -le 25.0) {
        return "GPS_OK"
    }
    return "GPS_WEAK"
}

function Get-GpsPairAccuracy($from, $to) {
    if ($null -eq $from.AccuracyM -and $null -eq $to.AccuracyM) {
        return $null
    }
    if ($null -eq $from.AccuracyM) {
        return [math]::Round([double]$to.AccuracyM, 0)
    }
    if ($null -eq $to.AccuracyM) {
        return [math]::Round([double]$from.AccuracyM, 0)
    }
    return [math]::Round([math]::Max([double]$from.AccuracyM, [double]$to.AccuracyM), 0)
}

function Get-GpsDistanceMeters([double]$lat1, [double]$lon1, [double]$lat2, [double]$lon2) {
    $earthRadiusM = 6371000.0
    $dLat = [math]::PI * ($lat2 - $lat1) / 180.0
    $dLon = [math]::PI * ($lon2 - $lon1) / 180.0
    $rLat1 = [math]::PI * $lat1 / 180.0
    $rLat2 = [math]::PI * $lat2 / 180.0
    $a = [math]::Sin($dLat / 2.0) * [math]::Sin($dLat / 2.0) +
        [math]::Cos($rLat1) * [math]::Cos($rLat2) *
        [math]::Sin($dLon / 2.0) * [math]::Sin($dLon / 2.0)
    $c = 2.0 * [math]::Atan2([math]::Sqrt($a), [math]::Sqrt(1.0 - $a))
    return [math]::Round($earthRadiusM * $c, 0)
}

function Get-GpsBearingDegrees([double]$lat1, [double]$lon1, [double]$lat2, [double]$lon2) {
    $rLat1 = [math]::PI * $lat1 / 180.0
    $rLat2 = [math]::PI * $lat2 / 180.0
    $dLon = [math]::PI * ($lon2 - $lon1) / 180.0
    $y = [math]::Sin($dLon) * [math]::Cos($rLat2)
    $x = [math]::Cos($rLat1) * [math]::Sin($rLat2) -
        [math]::Sin($rLat1) * [math]::Cos($rLat2) * [math]::Cos($dLon)
    $degrees = 180.0 * [math]::Atan2($y, $x) / [math]::PI
    if ($degrees -lt 0.0) {
        $degrees += 360.0
    }
    if ($degrees -ge 360.0) {
        $degrees -= 360.0
    }
    return [math]::Round($degrees, 0)
}

$gpsFixes = $locationEvents |
    Where-Object { $_.Event -eq "update" -or $_.Event -eq "last_known" } |
    ForEach-Object {
        $lat = Get-NumberOrNull $_.Lat
        $lon = Get-NumberOrNull $_.Lon
        if (-not (Test-ValidCoordinate $lat $lon)) {
            return
        }
        [pscustomobject]@{
            Window = $_.Window
            Bucket = $_.Bucket
            Device = $_.Device
            Time = $_.Time
            Provider = $_.Provider
            Lat = [double]$lat
            Lon = [double]$lon
            AccuracyM = Get-NumberOrNull $_.AccuracyM
        }
    } |
    Where-Object { $null -ne $_ } |
    Group-Object Window, Bucket, Device |
    ForEach-Object {
        $_.Group | Sort-Object Time | Select-Object -Last 1
    } |
    Sort-Object Window, Bucket, Device

$gpsDeviceVectors = New-Object System.Collections.Generic.List[object]
foreach ($fixGroup in ($gpsFixes | Group-Object Window, Bucket)) {
    $fixes = @($fixGroup.Group | Sort-Object Device)
    for ($i = 0; $i -lt $fixes.Count; $i++) {
        for ($j = 0; $j -lt $fixes.Count; $j++) {
            if ($i -eq $j) {
                continue
            }
            $from = $fixes[$i]
            $to = $fixes[$j]
            $gpsDeviceVectors.Add([pscustomobject]@{
                Window = $from.Window
                Bucket = $from.Bucket
                FromDevice = $from.Device
                ToDevice = $to.Device
                Status = Get-GpsPairStatus $from $to
                AccuracyM = Get-GpsPairAccuracy $from $to
                DistanceM = Get-GpsDistanceMeters $from.Lat $from.Lon $to.Lat $to.Lon
                BearingDeg = Get-GpsBearingDegrees $from.Lat $from.Lon $to.Lat $to.Lon
                FromProvider = $from.Provider
                ToProvider = $to.Provider
                FromTime = $from.Time.ToString("yyyy-MM-dd HH:mm:ss.fff")
                ToTime = $to.Time.ToString("yyyy-MM-dd HH:mm:ss.fff")
            })
        }
    }
}
$gpsDeviceVectors = $gpsDeviceVectors | Sort-Object Window, Bucket, FromDevice, ToDevice

$iffFieldCheckTimeline = $iffFieldChecks |
    Sort-Object Time, Device, PlayerId |
    ForEach-Object {
        [pscustomobject]@{
            Source = $_.Source
            SnapshotSource = $_.SnapshotSource
            Device = $_.Device
            LogFile = $_.LogFile
            Time = $_.Time.ToString("yyyy-MM-dd HH:mm:ss.fff")
            Window = $_.Window
            Bucket = $_.Bucket
            PlayerId = $_.PlayerId
            DisplayName = $_.DisplayName
            LocalDevicePlayerId = $_.LocalDevicePlayerId
            OfficeRole = $_.OfficeRole
            SelectedOfficeRole = $_.SelectedOfficeRole
            SelectedIsLocalDevice = $_.SelectedIsLocalDevice
            TrustedPlayer = $_.TrustedPlayer
            TrustLabel = $_.TrustLabel
            CombatState = $_.CombatState
            CombatAction = $_.CombatAction
            IdentityLabel = $_.IdentityLabel
            IdentityScore = $_.IdentityScore
            ProximityLabel = $_.ProximityLabel
            ProximityScore = $_.ProximityScore
            PositionLabel = $_.PositionLabel
            PositionScore = $_.PositionScore
            DirectionLabel = $_.DirectionLabel
            DirectionScore = $_.DirectionScore
            OperatorVerdict = $_.OperatorVerdict
            OfficeProximityVerdict = $_.OfficeProximityVerdict
            OfficeProximityDeltaDb = $_.OfficeProximityDeltaDb
            OfficeProximityReason = $_.OfficeProximityReason
            OfficeProximityA = $_.OfficeProximityA
            OfficeProximityB = $_.OfficeProximityB
            DistanceTargetPlayerId = $_.DistanceTargetPlayerId
            DistanceClass = $_.DistanceClass
            DistanceConfidence = $_.DistanceConfidence
            MovementTrend = $_.MovementTrend
            MovementConfidence = $_.MovementConfidence
            MovementRssiDeltaDb = $_.MovementRssiDeltaDb
            GpsStatus = $_.GpsStatus
            GpsAccuracyM = $_.GpsAccuracyM
            GpsDistanceM = $_.GpsDistanceM
            GpsBearingDeg = $_.GpsBearingDeg
            WitnessQuorum = $_.WitnessQuorum
            WitnessFreshSources = $_.WitnessFreshSources
            WitnessPossibleSources = $_.WitnessPossibleSources
            RemoteWitnessContract = $_.RemoteWitnessContract
            RemoteReportCount = $_.RemoteReportCount
            RemoteFreshSources = $_.RemoteFreshSources
            RemoteStaleSources = $_.RemoteStaleSources
            FieldRadioStatus = $_.FieldRadioStatus
            FieldRadioPolicy = $_.FieldRadioPolicy
            FieldRadioEnabled = $_.FieldRadioEnabled
            WifiFingerprintStatus = $_.WifiFingerprintStatus
            WifiRefreshRequested = $_.WifiRefreshRequested
            WifiRefreshAccepted = $_.WifiRefreshAccepted
            WifiFreshness = $_.WifiFreshness
            WifiFreshAgeMs = $_.WifiFreshAgeMs
            WifiFingerprint = $_.WifiFingerprint
            TransportStatus = $_.TransportStatus
            WitnessFreshness = $_.WitnessFreshness
            WitnessRssi = $_.WitnessRssi
            WitnessAgeMs = $_.WitnessAgeMs
            WitnessSsid = $_.WitnessSsid
            WitnessBssid = $_.WitnessBssid
            LocalApproach = $_.LocalApproach
        }
    }

function Get-LabelCountsText($items, [string]$propertyName) {
    $counts = @(
        $items |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_.$propertyName) } |
            Group-Object $propertyName |
            Sort-Object @{ Expression = "Count"; Descending = $true }, Name |
            ForEach-Object { "$($_.Name)=$($_.Count)" }
    )
    if ($counts.Count -eq 0) {
        return "none"
    }
    return $counts -join ", "
}

$iffFieldCheckSummary = $iffFieldChecks |
    Group-Object Window, Device, PlayerId |
    ForEach-Object {
        $items = @($_.Group | Sort-Object Time)
        $rssis = @($items | ForEach-Object { $_.WitnessRssi } | Where-Object { $null -ne $_ })
        $ages = @($items | ForEach-Object { $_.WitnessAgeMs } | Where-Object { $null -ne $_ })
        [pscustomobject]@{
            Window = $items[0].Window
            Device = $items[0].Device
            PlayerId = $items[0].PlayerId
            DisplayName = $items[0].DisplayName
            Count = $items.Count
            OfficeRoles = Get-LabelCountsText $items "OfficeRole"
            SelectedOfficeRoles = Get-LabelCountsText $items "SelectedOfficeRole"
            IdentityLabels = Get-LabelCountsText $items "IdentityLabel"
            ProximityLabels = Get-LabelCountsText $items "ProximityLabel"
            OperatorVerdicts = Get-LabelCountsText $items "OperatorVerdict"
            OfficeProximityVerdicts = Get-LabelCountsText $items "OfficeProximityVerdict"
            DistanceClasses = Get-LabelCountsText $items "DistanceClass"
            MovementTrends = Get-LabelCountsText $items "MovementTrend"
            GpsStatuses = Get-LabelCountsText $items "GpsStatus"
            TrustLabels = Get-LabelCountsText $items "TrustLabel"
            CombatStates = Get-LabelCountsText $items "CombatState"
            CombatActions = Get-LabelCountsText $items "CombatAction"
            WitnessQuorumLabels = Get-LabelCountsText $items "WitnessQuorum"
            RemoteWitnessContracts = Get-LabelCountsText $items "RemoteWitnessContract"
            WitnessFreshness = Get-LabelCountsText $items "WitnessFreshness"
            AvgIdentityScore = [math]::Round(($items | Measure-Object IdentityScore -Average).Average, 1)
            AvgProximityScore = [math]::Round(($items | Measure-Object ProximityScore -Average).Average, 1)
            MaxRemoteReportCount = ($items | Measure-Object RemoteReportCount -Maximum).Maximum
            MaxRemoteFreshSources = ($items | Measure-Object RemoteFreshSources -Maximum).Maximum
            MaxRemoteStaleSources = ($items | Measure-Object RemoteStaleSources -Maximum).Maximum
            AvgRssi = if ($rssis.Count -gt 0) { [math]::Round(($rssis | Measure-Object -Average).Average, 1) } else { $null }
            MinRssi = if ($rssis.Count -gt 0) { ($rssis | Measure-Object -Minimum).Minimum } else { $null }
            MaxRssi = if ($rssis.Count -gt 0) { ($rssis | Measure-Object -Maximum).Maximum } else { $null }
            AvgAgeMs = if ($ages.Count -gt 0) { [math]::Round(($ages | Measure-Object -Average).Average, 0) } else { $null }
            FirstCheck = $items[0].Time.ToString("yyyy-MM-dd HH:mm:ss.fff")
            LastCheck = $items[$items.Count - 1].Time.ToString("yyyy-MM-dd HH:mm:ss.fff")
        }
    } |
    Sort-Object Window, Device, PlayerId

function Get-OfficeWallShadowSide([string]$verdict, [string]$side) {
    if ($verdict -eq "ONLY_A_VISIBLE" -and $side -eq "visible") {
        return "A"
    }
    if ($verdict -eq "ONLY_A_VISIBLE" -and $side -eq "missing") {
        return "B"
    }
    if ($verdict -eq "ONLY_B_VISIBLE" -and $side -eq "visible") {
        return "B"
    }
    if ($verdict -eq "ONLY_B_VISIBLE" -and $side -eq "missing") {
        return "A"
    }
    return ""
}

$officeWallShadowSummary = $iffFieldChecks |
    Where-Object { $_.OfficeProximityVerdict -eq "ONLY_A_VISIBLE" -or $_.OfficeProximityVerdict -eq "ONLY_B_VISIBLE" } |
    Group-Object Window, Device, LocalDevicePlayerId, OfficeProximityVerdict |
    ForEach-Object {
        $items = @($_.Group | Sort-Object Time)
        $verdict = $items[0].OfficeProximityVerdict
        [pscustomobject]@{
            Window = $items[0].Window
            Device = $items[0].Device
            LocalDevicePlayerId = $items[0].LocalDevicePlayerId
            OfficeRole = $items[0].OfficeRole
            Verdict = $verdict
            VisibleSide = Get-OfficeWallShadowSide $verdict "visible"
            MissingSide = Get-OfficeWallShadowSide $verdict "missing"
            Count = $items.Count
            FirstCheck = $items[0].Time.ToString("yyyy-MM-dd HH:mm:ss.fff")
            LastCheck = $items[$items.Count - 1].Time.ToString("yyyy-MM-dd HH:mm:ss.fff")
            FirstReason = $items[0].OfficeProximityReason
            LastReason = $items[$items.Count - 1].OfficeProximityReason
        }
    } |
    Sort-Object Window, Device, Verdict

$bleRssiTimeline = $bleRssiEvents |
    Sort-Object Time, Device, LocalPlayerId, SeenPlayerId |
    ForEach-Object {
        [pscustomobject]@{
            Device = $_.Device
            LogFile = $_.LogFile
            Time = $_.Time.ToString("yyyy-MM-dd HH:mm:ss.fff")
            Window = $_.Window
            Bucket = $_.Bucket
            LocalPlayerId = $_.LocalPlayerId
            SeenPlayerId = $_.SeenPlayerId
            Address = $_.Address
            Rssi = $_.Rssi
            IsOutlier127 = $_.IsOutlier127
            Contract = $_.Contract
        }
    }

$bleRssiSummary = $bleRssiEvents |
    Group-Object Window, Bucket, Device, LocalPlayerId, SeenPlayerId |
    ForEach-Object {
        $items = @($_.Group | Sort-Object Time)
        $valid = @($items | Where-Object { $_.Rssi -ne 127 })
        [pscustomobject]@{
            Window = $items[0].Window
            Bucket = $items[0].Bucket
            Device = $items[0].Device
            LocalPlayerId = $items[0].LocalPlayerId
            SeenPlayerId = $items[0].SeenPlayerId
            Count = $items.Count
            ValidCount = $valid.Count
            Outlier127 = ($items.Count - $valid.Count)
            AvgRssi = if ($valid.Count -gt 0) { [math]::Round(($valid | Measure-Object Rssi -Average).Average, 1) } else { $null }
            MinRssi = if ($valid.Count -gt 0) { ($valid | Measure-Object Rssi -Minimum).Minimum } else { $null }
            MaxRssi = if ($valid.Count -gt 0) { ($valid | Measure-Object Rssi -Maximum).Maximum } else { $null }
            FirstSeen = $items[0].Time.ToString("yyyy-MM-dd HH:mm:ss.fff")
            LastSeen = $items[$items.Count - 1].Time.ToString("yyyy-MM-dd HH:mm:ss.fff")
        }
    } |
    Sort-Object Window, Bucket, Device, LocalPlayerId, SeenPlayerId

function Test-OfficeCalibrationEnabled {
    return -not [double]::IsNaN($OfficeBaselineSideA) -and -not [double]::IsNaN($OfficeBaselineSideB)
}

function Get-OfficeCalibrationLabel {
    if (-not (Test-OfficeCalibrationEnabled)) {
        return "none"
    }
    return "sideA=$OfficeBaselineSideA sideB=$OfficeBaselineSideB"
}

function Get-OfficeProximityVerdictFromDelta([Nullable[double]]$delta) {
    if ($null -eq $delta) {
        return "INSUFFICIENT_DATA"
    }
    if ($delta -ge 8) {
        return "CLOSER_TO_A"
    }
    if ($delta -le -8) {
        return "CLOSER_TO_B"
    }
    return "BETWEEN_OR_AMBIGUOUS"
}

function Get-OfficeProximityVerdict([Nullable[double]]$sideARssi, [Nullable[double]]$sideBRssi) {
    if ($null -eq $sideARssi -or $null -eq $sideBRssi) {
        return "INSUFFICIENT_DATA"
    }
    return Get-OfficeProximityVerdictFromDelta ($sideARssi - $sideBRssi)
}

$officeProximityVerdicts = $bleRssiSummary |
    Where-Object { $_.LocalPlayerId -eq "petya" } |
    Group-Object Window, Bucket, Device, LocalPlayerId |
    ForEach-Object {
        $items = @($_.Group)
        $sideA = $items | Where-Object SeenPlayerId -eq "vasya" | Select-Object -First 1
        $sideB = $items | Where-Object SeenPlayerId -eq "zhenya" | Select-Object -First 1
        $sideARssi = if ($sideA -and $sideA.ValidCount -gt 0) { [double]$sideA.AvgRssi } else { $null }
        $sideBRssi = if ($sideB -and $sideB.ValidCount -gt 0) { [double]$sideB.AvgRssi } else { $null }
        $rawDeltaDb = if ($null -ne $sideARssi -and $null -ne $sideBRssi) { [math]::Round($sideARssi - $sideBRssi, 1) } else { $null }
        $deltaDb = $rawDeltaDb
        if ((Test-OfficeCalibrationEnabled) -and $null -ne $sideARssi -and $null -ne $sideBRssi) {
            $deltaDb = [math]::Round(($sideARssi - $OfficeBaselineSideA) - ($sideBRssi - $OfficeBaselineSideB), 1)
        }
        [pscustomobject]@{
            Window = $items[0].Window
            Bucket = $items[0].Bucket
            Device = $items[0].Device
            LocalPlayerId = $items[0].LocalPlayerId
            SideAPlayerId = "vasya"
            SideBPlayerId = "zhenya"
            SideARssi = $sideARssi
            SideBRssi = $sideBRssi
            DeltaDb = $deltaDb
            Verdict = Get-OfficeProximityVerdictFromDelta $deltaDb
            RawDeltaDb = $rawDeltaDb
            RawVerdict = Get-OfficeProximityVerdictFromDelta $rawDeltaDb
            Calibration = Get-OfficeCalibrationLabel
            SideAValidCount = if ($sideA) { $sideA.ValidCount } else { 0 }
            SideBValidCount = if ($sideB) { $sideB.ValidCount } else { 0 }
            SideAOutlier127 = if ($sideA) { $sideA.Outlier127 } else { 0 }
            SideBOutlier127 = if ($sideB) { $sideB.Outlier127 } else { 0 }
        }
    } |
    Sort-Object Window, Bucket, Device, LocalPlayerId

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

function New-AnalysisKey($parts) {
    return ($parts | ForEach-Object { [string]$_ }) -join "`t"
}

function Add-ListItem($map, [string]$key, $item) {
    if (-not $map.ContainsKey($key)) {
        $map[$key] = New-Object System.Collections.Generic.List[object]
    }
    $map[$key].Add($item)
}

function Add-UniqueListValue($map, [string]$key, [string]$value) {
    if (-not $map.ContainsKey($key)) {
        $map[$key] = New-Object System.Collections.Generic.List[string]
    }
    if (-not $map[$key].Contains($value)) {
        $map[$key].Add($value)
    }
}

function Get-ZoneScoreFromFingerprintMap($bucketItems, $fingerprintByBssid, [int]$fingerprintBssidCount) {
    $sharedCount = 0
    $errorSum = 0.0
    foreach ($bucketItem in $bucketItems) {
        $fingerprintItem = $fingerprintByBssid[$bucketItem.Bssid]
        if ($null -ne $fingerprintItem) {
            $sharedCount++
            $errorSum += [math]::Abs([double]$bucketItem.AvgRssi - [double]$fingerprintItem.AvgRssi)
        }
    }

    $bucketBssidCount = $bucketItems.Count
    $avgAbsError = if ($sharedCount -gt 0) { [math]::Round($errorSum / $sharedCount, 2) } else { 99.0 }
    $overlapBucket = if ($bucketBssidCount -gt 0) { [math]::Round($sharedCount / $bucketBssidCount, 3) } else { 0 }
    $overlapFingerprint = if ($fingerprintBssidCount -gt 0) { [math]::Round($sharedCount / $fingerprintBssidCount, 3) } else { 0 }
    $score = [math]::Round(($sharedCount * 2.0) + ($overlapBucket * 20.0) + ($overlapFingerprint * 10.0) - $avgAbsError, 3)

    [pscustomobject]@{
        Score = $score
        SharedBssid = $sharedCount
        BucketBssid = $bucketBssidCount
        FingerprintBssid = $fingerprintBssidCount
        OverlapBucket = $overlapBucket
        OverlapFingerprint = $overlapFingerprint
        AvgAbsRssiError = $avgAbsError
    }
}

$actualWindowByBucketDevice = @{}
foreach ($entry in $scanEntries) {
    $key = New-AnalysisKey @($entry.Bucket, $entry.Device)
    if (-not $actualWindowByBucketDevice.ContainsKey($key)) {
        $actualWindowByBucketDevice[$key] = $entry.Window
    }
}

$fingerprintsByDeviceZone = @{}
$fingerprintByDeviceZoneBssid = @{}
$zonesByDevice = @{}
foreach ($row in $fingerprints) {
    $deviceZoneKey = New-AnalysisKey @($row.Device, $row.Zone)
    Add-ListItem $fingerprintsByDeviceZone $deviceZoneKey $row
    $fingerprintByDeviceZoneBssid[(New-AnalysisKey @($row.Device, $row.Zone, $row.Bssid))] = $row
    Add-UniqueListValue $zonesByDevice $row.Device $row.Zone
}

$zoneEvaluation = New-Object System.Collections.Generic.List[object]
$bucketGroups = $bucketSummary |
    Where-Object { $_.Bucket -ne "pre_test" } |
    Group-Object Bucket, Device

foreach ($bucketGroup in $bucketGroups) {
    $bucketItems = $bucketGroup.Group
    $device = $bucketItems[0].Device
    $bucket = $bucketItems[0].Bucket
    $actualWindow = $actualWindowByBucketDevice[(New-AnalysisKey @($bucket, $device))]
    $zones = $zonesByDevice[$device]
    foreach ($zone in $zones) {
        $fingerprintRows = $fingerprintsByDeviceZone[(New-AnalysisKey @($device, $zone))]
        $fingerprintByBssid = @{}
        foreach ($fingerprintItem in $fingerprintRows) {
            $fingerprintByBssid[$fingerprintItem.Bssid] = $fingerprintItem
        }
        $score = Get-ZoneScoreFromFingerprintMap $bucketItems $fingerprintByBssid $fingerprintRows.Count
        $zoneEvaluation.Add([pscustomobject]@{
            Bucket = $bucket
            Device = $device
            ActualWindow = $actualWindow
            CandidateZone = $zone
            Score = $score.Score
            SharedBssid = $score.SharedBssid
            BucketBssid = $score.BucketBssid
            FingerprintBssid = $score.FingerprintBssid
            OverlapBucket = $score.OverlapBucket
            OverlapFingerprint = $score.OverlapFingerprint
            AvgAbsRssiError = $score.AvgAbsRssiError
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

$zoneRawAggregatesByDeviceZone = @{}
$zoneBucketRawAggregateByKey = @{}
foreach ($scanEntry in $scanEntries) {
    if ($scanEntry.Window -eq "pre_test" -or $scanEntry.Window -eq "post_windows") {
        continue
    }
    $zoneKey = New-AnalysisKey @($scanEntry.Device, $scanEntry.Window, $scanEntry.Bssid)
    if (-not $zoneBucketRawAggregateByKey.ContainsKey($zoneKey)) {
        $aggregate = [pscustomobject]@{
            Device = $scanEntry.Device
            Zone = $scanEntry.Window
            Bssid = $scanEntry.Bssid
            Ssid = $scanEntry.Ssid
            Count = 0
            SumRssi = 0.0
        }
        $zoneBucketRawAggregateByKey[$zoneKey] = $aggregate
        Add-ListItem $zoneRawAggregatesByDeviceZone (New-AnalysisKey @($scanEntry.Device, $scanEntry.Window)) $aggregate
    }
    $zoneBucketRawAggregateByKey[$zoneKey].Count++
    $zoneBucketRawAggregateByKey[$zoneKey].SumRssi += [double]$scanEntry.Rssi

    $bucketZoneKey = New-AnalysisKey @($scanEntry.Device, $scanEntry.Bucket, $scanEntry.Window, $scanEntry.Bssid)
    if (-not $zoneBucketRawAggregateByKey.ContainsKey($bucketZoneKey)) {
        $zoneBucketRawAggregateByKey[$bucketZoneKey] = [pscustomobject]@{
            Count = 0
            SumRssi = 0.0
        }
    }
    $zoneBucketRawAggregateByKey[$bucketZoneKey].Count++
    $zoneBucketRawAggregateByKey[$bucketZoneKey].SumRssi += [double]$scanEntry.Rssi
}

$crossValidationEvaluation = New-Object System.Collections.Generic.List[object]
foreach ($bucketGroup in $bucketGroups) {
    $bucketItems = $bucketGroup.Group
    $device = $bucketItems[0].Device
    $bucket = $bucketItems[0].Bucket
    $actualWindow = $actualWindowByBucketDevice[(New-AnalysisKey @($bucket, $device))]
    $zones = $zonesByDevice[$device]
    foreach ($zone in $zones) {
        $zoneAggregates = $zoneRawAggregatesByDeviceZone[(New-AnalysisKey @($device, $zone))]
        $trainingFingerprintByBssid = @{}
        foreach ($aggregate in $zoneAggregates) {
            $excluded = $zoneBucketRawAggregateByKey[(New-AnalysisKey @($device, $bucket, $zone, $aggregate.Bssid))]
            $excludedCount = if ($null -ne $excluded) { $excluded.Count } else { 0 }
            $excludedSum = if ($null -ne $excluded) { $excluded.SumRssi } else { 0.0 }
            $trainingCount = $aggregate.Count - $excludedCount
            if ($trainingCount -lt 3) {
                continue
            }
            $trainingFingerprintByBssid[$aggregate.Bssid] = [pscustomobject]@{
                Bssid = $aggregate.Bssid
                AvgRssi = [math]::Round(($aggregate.SumRssi - $excludedSum) / $trainingCount, 1)
            }
        }

        $score = Get-ZoneScoreFromFingerprintMap $bucketItems $trainingFingerprintByBssid $trainingFingerprintByBssid.Count
        $crossValidationEvaluation.Add([pscustomobject]@{
            Bucket = $bucket
            Device = $device
            ActualWindow = $actualWindow
            CandidateZone = $zone
            Score = $score.Score
            SharedBssid = $score.SharedBssid
            BucketBssid = $score.BucketBssid
            FingerprintBssid = $score.FingerprintBssid
            OverlapBucket = $score.OverlapBucket
            OverlapFingerprint = $score.OverlapFingerprint
            AvgAbsRssiError = $score.AvgAbsRssiError
        })
    }
}

$crossValidationPredictions = $crossValidationEvaluation |
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
$beaconTimeline | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "beacon-timeline.csv")
$beaconBucketSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "beacon-bucket-summary.csv")
$beaconSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "beacon-summary.csv")
$eventSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "event-summary.csv")
$freshnessTicks | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "freshness-timeline.csv")
$freshnessSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "freshness-summary.csv")
$sensorEvents | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "sensor-timeline.csv")
$sensorSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "sensor-summary.csv")
$locationEvents | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "location-timeline.csv")
$locationSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "location-summary.csv")
$gpsDeviceVectors | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "gps-device-vectors.csv")
$iffFieldCheckTimeline | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "iff-field-checks.csv")
$iffFieldCheckSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "iff-field-check-summary.csv")
$officeWallShadowSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "office-wall-shadow-summary.csv")
$bleRssiTimeline | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "ble-rssi-timeline.csv")
$bleRssiSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "ble-rssi-summary.csv")
$officeProximityVerdicts | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "office-proximity-verdict.csv")
$windowDeviceSummary | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "window-device-summary.csv")
$comparison | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "device-comparison.csv")
$contexts | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "device-context.csv")
$movementDeltas | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "movement-deltas.csv")
$fingerprints | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "zone-fingerprints.csv")
$zoneEvaluation | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "zone-evaluation.csv")
$zonePredictions | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "zone-predictions.csv")
$crossValidationEvaluation | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "zone-cross-validation-evaluation.csv")
$crossValidationPredictions | Export-Csv -NoTypeInformation -Encoding UTF8 (Join-Path $OutputDir "zone-cross-validation-predictions.csv")

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
$report.Add("## Wi-Fi Freshness Summary")
$report.Add("")
if ($freshnessSummary.Count -eq 0) {
    $report.Add("No `WIFI_DIAG event=tick` lines found. Rebuild and rerun the app with Phase 6 logging.")
} else {
    $report.Add("| Window | Device | Ticks | Receiver Updated | Avg Fresh ms | Max Fresh ms | >3s | >5s | No Fresh | Avg Cached | Avg Gap s | Max Gap s |")
    $report.Add("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
    foreach ($row in $freshnessSummary) {
        $report.Add("| $($row.Window) | $($row.Device) | $($row.TickCount) | $($row.ReceiverUpdated) | $($row.AvgFreshAgeMs) | $($row.MaxFreshAgeMs) | $($row.StaleOver3000) | $($row.StaleOver5000) | $($row.NoFreshYet) | $($row.AvgCachedCount) | $($row.AvgReceiverGapSec) | $($row.MaxReceiverGapSec) |")
    }
}
$report.Add("")
$report.Add("## Beacon Summary")
$report.Add("")
if ($beaconSummary.Count -eq 0) {
    $patterns = if ($beaconSsidPatterns.Count -eq 0) { "(none)" } else { $beaconSsidPatterns -join ", " }
    $report.Add("No beacon SSID entries matched: ``$patterns``.")
} else {
    $report.Add("Matched beacon SSID patterns: ``$($beaconSsidPatterns -join ', ')``.")
    $report.Add("")
    $report.Add("| Window | Device | SSID | BSSID | Count | Avg RSSI | Min | Max | Range counts |")
    $report.Add("| --- | --- | --- | --- | ---: | ---: | ---: | ---: | --- |")
    foreach ($row in $beaconSummary) {
        $ssid = ($row.Ssid -replace '\|', '/')
        $rangeCounts = "vc=$($row.VeryClose), c=$($row.Close), m=$($row.Medium), far=$($row.Far), edge=$($row.Edge)"
        $report.Add("| $($row.Window) | $($row.Device) | $ssid | $($row.Bssid) | $($row.Count) | $($row.AvgRssi) | $($row.MinRssi) | $($row.MaxRssi) | $rangeCounts |")
    }
    $report.Add("")
    $report.Add("| Bucket | Device | SSID | Avg RSSI | Range | Trend | Delta dB |")
    $report.Add("| --- | --- | --- | ---: | --- | --- | ---: |")
    foreach ($row in ($beaconBucketSummary | Select-Object -First 40)) {
        $ssid = ($row.Ssid -replace '\|', '/')
        $report.Add("| $($row.Bucket) | $($row.Device) | $ssid | $($row.AvgRssi) | $($row.RangeClass) | $($row.Trend) | $($row.TrendDb) |")
    }
}
$report.Add("")
$report.Add("## BLE RSSI Summary")
$report.Add("")
if ($bleRssiSummary.Count -eq 0) {
    $report.Add("No `IFF_DIAG event=ble_field_radio_rx` lines found.")
} else {
    $report.Add("| Window | Bucket | Device | Local | Seen | Count | Valid | Outlier 127 | Avg RSSI | Min | Max | First | Last |")
    $report.Add("| --- | --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |")
    foreach ($row in $bleRssiSummary) {
        $avg = if ($null -eq $row.AvgRssi) { "" } else { $row.AvgRssi }
        $min = if ($null -eq $row.MinRssi) { "" } else { $row.MinRssi }
        $max = if ($null -eq $row.MaxRssi) { "" } else { $row.MaxRssi }
        $report.Add("| $($row.Window) | $($row.Bucket) | $($row.Device) | $($row.LocalPlayerId) | $($row.SeenPlayerId) | $($row.Count) | $($row.ValidCount) | $($row.Outlier127) | $avg | $min | $max | $($row.FirstSeen) | $($row.LastSeen) |")
    }
    if (($bleRssiSummary | Where-Object { $_.Outlier127 -gt 0 }).Count -gt 0) {
        $report.Add("")
        $report.Add("BLE RSSI value `127` is treated as an outlier and excluded from Avg/Min/Max.")
    }
}
$report.Add("")
$report.Add("## Office Proximity Verdict")
$report.Add("")
if ($officeProximityVerdicts.Count -eq 0) {
    $report.Add("No local `petya` BLE buckets found for retrospective A/B proximity verdict.")
} else {
    $report.Add("Retrospective verdict compares C/Petya hearing A/Vasya against C/Petya hearing B/Zhenya per bucket.")
    $report.Add("")
    $report.Add("| Window | Bucket | Device | Local | A RSSI | B RSSI | Delta dB | Verdict | Raw delta | Raw verdict | Calibration | A valid | B valid | A outlier 127 | B outlier 127 |")
    $report.Add("| --- | --- | --- | --- | ---: | ---: | ---: | --- | ---: | --- | --- | ---: | ---: | ---: | ---: |")
    foreach ($row in $officeProximityVerdicts) {
        $sideA = if ($null -eq $row.SideARssi) { "" } else { $row.SideARssi }
        $sideB = if ($null -eq $row.SideBRssi) { "" } else { $row.SideBRssi }
        $delta = if ($null -eq $row.DeltaDb) { "" } else { $row.DeltaDb }
        $rawDelta = if ($null -eq $row.RawDeltaDb) { "" } else { $row.RawDeltaDb }
        $report.Add("| $($row.Window) | $($row.Bucket) | $($row.Device) | $($row.LocalPlayerId) | $sideA | $sideB | $delta | $($row.Verdict) | $rawDelta | $($row.RawVerdict) | $($row.Calibration) | $($row.SideAValidCount) | $($row.SideBValidCount) | $($row.SideAOutlier127) | $($row.SideBOutlier127) |")
    }
}
$report.Add("")
$report.Add("## Office Wall Shadow Summary")
$report.Add("")
if ($officeWallShadowSummary.Count -eq 0) {
    $report.Add("No one-sided office visibility verdicts found.")
} else {
    $report.Add("Wall-shadow verdicts mean the moving phone has usable fresh BLE evidence from only one office side. Treat them as partial position evidence, not as missing data.")
    $report.Add("")
    $report.Add("| Window | Device | Player | Role | Verdict | Visible | Missing | Count | First | Last | Reason |")
    $report.Add("| --- | --- | --- | --- | --- | --- | --- | ---: | --- | --- | --- |")
    foreach ($row in $officeWallShadowSummary) {
        $reason = ($row.LastReason -replace '\|', '/')
        $report.Add("| $($row.Window) | $($row.Device) | $($row.LocalDevicePlayerId) | $($row.OfficeRole) | $($row.Verdict) | $($row.VisibleSide) | $($row.MissingSide) | $($row.Count) | $($row.FirstCheck) | $($row.LastCheck) | $reason |")
    }
}
$report.Add("")
$report.Add("## IFF Field Checks")
$report.Add("")
if ($iffFieldCheckTimeline.Count -eq 0) {
    $report.Add("No `IFF_DIAG event=field_check` or `IFF_DIAG event=auto_field_check` lines found. Tap the IFF record button or enable automatic field snapshots during field checks.")
} else {
    $report.Add("| Source | Time | Window | Device | Office Role | Player | This Device | Trust | Combat | Operator | Office Proximity | Identity | Proximity | Quorum | Remote | Field Radio | Field Radio Policy | UDP Debug | Witness | RSSI | Age ms | Position | Direction |")
    $report.Add("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | ---: | ---: | --- | --- |")
    foreach ($row in $iffFieldCheckTimeline) {
        $player = if ([string]::IsNullOrWhiteSpace($row.DisplayName)) { $row.PlayerId } else { "$($row.DisplayName) ($($row.PlayerId))" }
        $rssi = if ($null -eq $row.WitnessRssi) { "" } else { $row.WitnessRssi }
        $age = if ($null -eq $row.WitnessAgeMs) { "" } else { $row.WitnessAgeMs }
        $quorum = if ([string]::IsNullOrWhiteSpace($row.WitnessQuorum)) { "" } else { "$($row.WitnessQuorum) $($row.WitnessFreshSources)/$($row.WitnessPossibleSources)" }
        $remoteStale = if ($null -eq $row.RemoteStaleSources) { 0 } else { $row.RemoteStaleSources }
        $remote = if ([string]::IsNullOrWhiteSpace($row.RemoteWitnessContract)) { "" } else { "$($row.RemoteReportCount) reports / $($row.RemoteFreshSources) fresh / $remoteStale stale" }
        $officeRole = if ([string]::IsNullOrWhiteSpace($row.OfficeRole)) { "" } else { "$($row.OfficeRole) / selected=$($row.SelectedOfficeRole)" }
        $localDevice = if ([string]::IsNullOrWhiteSpace($row.LocalDevicePlayerId)) { "" } else { "$($row.LocalDevicePlayerId) / selected=$($row.SelectedIsLocalDevice)" }
        $fieldRadio = if ([string]::IsNullOrWhiteSpace($row.FieldRadioEnabled)) { $row.FieldRadioStatus } else { "$($row.FieldRadioEnabled) / $($row.FieldRadioStatus)" }
        $trust = if ([string]::IsNullOrWhiteSpace($row.TrustLabel)) { "" } else { "$($row.TrustLabel) / trusted=$($row.TrustedPlayer)" }
        $combat = if ([string]::IsNullOrWhiteSpace($row.CombatState)) { "" } else { "$($row.CombatState) / $($row.CombatAction)" }
        $officeProximity = if ([string]::IsNullOrWhiteSpace($row.OfficeProximityVerdict)) { "" } else { "$($row.OfficeProximityVerdict) delta=$($row.OfficeProximityDeltaDb)dB" }
        $report.Add("| $($row.Source) | $($row.Time) | $($row.Window) | $($row.Device) | $officeRole | $player | $localDevice | $trust | $combat | $($row.OperatorVerdict) | $officeProximity | $($row.IdentityLabel) $($row.IdentityScore) | $($row.ProximityLabel) $($row.ProximityScore) | $quorum | $remote | $fieldRadio | $($row.FieldRadioPolicy) | $($row.TransportStatus) | $($row.WitnessFreshness) | $rssi | $age | $($row.PositionLabel) $($row.PositionScore) | $($row.DirectionLabel) $($row.DirectionScore) |")
    }

    $report.Add("")
    $report.Add("| Window | Device | Office Roles | Player | Checks | Trust labels | Combat states | Combat actions | Operator verdicts | Office proximity | Identity labels | Proximity labels | Quorum labels | Remote contract | Remote max | Witness | Avg RSSI | Avg age ms |")
    $report.Add("| --- | --- | --- | --- | ---: | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | ---: | ---: |")
    foreach ($row in $iffFieldCheckSummary) {
        $avgRssi = if ($null -eq $row.AvgRssi) { "" } else { $row.AvgRssi }
        $avgAge = if ($null -eq $row.AvgAgeMs) { "" } else { $row.AvgAgeMs }
        $remoteMaxStale = if ($null -eq $row.MaxRemoteStaleSources) { 0 } else { $row.MaxRemoteStaleSources }
        $remoteMax = if ($null -eq $row.MaxRemoteReportCount) { "" } else { "$($row.MaxRemoteReportCount) reports / $($row.MaxRemoteFreshSources) fresh / $remoteMaxStale stale" }
        $report.Add("| $($row.Window) | $($row.Device) | $($row.OfficeRoles) | $($row.DisplayName) ($($row.PlayerId)) | $($row.Count) | $($row.TrustLabels) | $($row.CombatStates) | $($row.CombatActions) | $($row.OperatorVerdicts) | $($row.OfficeProximityVerdicts) | $($row.IdentityLabels) | $($row.ProximityLabels) | $($row.WitnessQuorumLabels) | $($row.RemoteWitnessContracts) | $remoteMax | $($row.WitnessFreshness) | $avgRssi | $avgAge |")
    }

    $report.Add("")
    $report.Add("Threshold notes:")
    foreach ($group in ($iffFieldChecks | Group-Object ProximityLabel | Sort-Object Name)) {
        $items = $group.Group
        $rssis = @($items | ForEach-Object { $_.WitnessRssi } | Where-Object { $null -ne $_ })
        $ages = @($items | ForEach-Object { $_.WitnessAgeMs } | Where-Object { $null -ne $_ })
        $rssiText = if ($rssis.Count -gt 0) {
            "RSSI avg $([math]::Round(($rssis | Measure-Object -Average).Average, 1)), min $(($rssis | Measure-Object -Minimum).Minimum), max $(($rssis | Measure-Object -Maximum).Maximum)"
        } else {
            "no RSSI"
        }
        $ageText = if ($ages.Count -gt 0) {
            "age avg $([math]::Round(($ages | Measure-Object -Average).Average, 0)) ms"
        } else {
            "no age"
        }
        $report.Add("- ``$($group.Name)``: $($items.Count) check(s), $rssiText, $ageText.")
    }
    $report.Add("- Current field evidence supports RSSI as a coarse proximity hint only. Keep direction independent: radio freshness does not provide azimuth.")
    $report.Add("- `RADIO_NEAR` can mean strong short-range evidence; `RADIO_MID` is a weaker proximity hint; stale or missing radio keeps identity and proximity below confirmation.")
}
$report.Add("")
$report.Add("## Sensor Summary")
$report.Add("")
if ($sensorSummary.Count -eq 0) {
    $report.Add("No `SENSOR_DIAG` lines found. Rebuild and rerun the app with sensor diagnostics.")
} else {
    $report.Add("| Window | Device | Ticks | Events | Registered | Unavailable | Yaw | Pressure | Light | Steps |")
    $report.Add("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
    foreach ($row in $sensorSummary) {
        $report.Add("| $($row.Window) | $($row.Device) | $($row.TickCount) | $($row.SensorEvents) | $($row.RegisteredSensors) | $($row.UnavailableSensors) | $($row.YawSamples) | $($row.PressureSamples) | $($row.LightSamples) | $($row.StepSamples) |")
    }
}
$report.Add("")
$report.Add("## Location Summary")
$report.Add("")
if ($locationSummary.Count -eq 0) {
    $report.Add("No `LOCATION_DIAG` lines found. Rebuild and rerun the app with location diagnostics.")
} else {
    $report.Add("| Window | Device | Events | Updates | Last Known | GPS | Network | Best Accuracy m | Avg Accuracy m |")
    $report.Add("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
    foreach ($row in $locationSummary) {
        $report.Add("| $($row.Window) | $($row.Device) | $($row.LocationEvents) | $($row.Updates) | $($row.LastKnown) | $($row.GpsUpdates) | $($row.NetworkUpdates) | $($row.BestAccuracyM) | $($row.AvgAccuracyM) |")
    }
}
$report.Add("")
$report.Add("## GPS Device Vectors")
$report.Add("")
if ($gpsDeviceVectors.Count -eq 0) {
    $report.Add("No bucket-level GPS vectors found. Need at least two devices with valid `LOCATION_DIAG` coordinates in the same bucket.")
} else {
    $report.Add("Directed vectors are computed from each device's latest valid GPS fix in the bucket.")
    $report.Add("")
    $report.Add("| Window | Bucket | From | To | Status | Accuracy m | Distance m | Bearing deg |")
    $report.Add("| --- | --- | --- | --- | --- | ---: | ---: | ---: |")
    foreach ($row in ($gpsDeviceVectors | Select-Object -First 60)) {
        $accuracy = if ($null -eq $row.AccuracyM) { "" } else { $row.AccuracyM }
        $report.Add("| $($row.Window) | $($row.Bucket) | $($row.FromDevice) | $($row.ToDevice) | $($row.Status) | $accuracy | $($row.DistanceM) | $($row.BearingDeg) |")
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
$zonePredictionRows = @($zonePredictions | Where-Object { $null -ne $_ -and -not [string]::IsNullOrWhiteSpace($_.Bucket) })
if ($zonePredictionRows.Count -eq 0) {
    $report.Add("No zone predictions generated.")
} else {
    $correct = ($zonePredictionRows | Where-Object Correct -eq $true).Count
    $total = $zonePredictionRows.Count
    $accuracy = [math]::Round(($correct * 100.0) / $total, 1)
    $report.Add("Best-zone bucket accuracy against named windows: $correct/$total ($accuracy%).")
    $report.Add("")
    $report.Add("| Bucket | Device | Actual | Predicted | Correct | Shared | Error | Score |")
    $report.Add("| --- | --- | --- | --- | --- | ---: | ---: | ---: |")
    foreach ($row in $zonePredictionRows) {
        $report.Add("| $($row.Bucket) | $($row.Device) | $($row.ActualWindow) | $($row.PredictedZone) | $($row.Correct) | $($row.SharedBssid) | $($row.AvgAbsRssiError) | $($row.Score) |")
    }
}
$report.Add("")
$report.Add("## Cross-Validated Zone Evaluation")
$report.Add("")
$crossValidationPredictionRows = @($crossValidationPredictions | Where-Object { $null -ne $_ -and -not [string]::IsNullOrWhiteSpace($_.Bucket) })
if ($crossValidationPredictionRows.Count -eq 0) {
    $report.Add("No cross-validation predictions generated.")
} else {
    $cvCorrect = ($crossValidationPredictionRows | Where-Object Correct -eq $true).Count
    $cvTotal = $crossValidationPredictionRows.Count
    $cvAccuracy = [math]::Round(($cvCorrect * 100.0) / $cvTotal, 1)
    $report.Add("Leave-one-bucket-out accuracy: $cvCorrect/$cvTotal ($cvAccuracy%).")
    $report.Add("")
    $report.Add("| Bucket | Device | Actual | Predicted | Correct | Shared | Error | Score |")
    $report.Add("| --- | --- | --- | --- | --- | ---: | ---: | ---: |")
    foreach ($row in $crossValidationPredictionRows) {
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
