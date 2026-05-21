param(
    [Parameter(Mandatory = $true)]
    [string]$Serial,

    [Parameter(Mandatory = $true)]
    [ValidateSet("A", "B", "C", "Operator")]
    [string]$Role,

    [string]$ApkPath = "app\build\outputs\apk\debug\app-debug.apk",

    [switch]$SkipInstall
)

$ErrorActionPreference = "Stop"

function Get-RoleConfig([string]$roleName) {
    switch ($roleName) {
        "A" {
            return [pscustomobject]@{
                PlayerId = "vasya"
                OfficeRole = "PHONE_A_WITNESS"
                TrustPetya = $true
            }
        }
        "B" {
            return [pscustomobject]@{
                PlayerId = "zhenya"
                OfficeRole = "PHONE_B_WITNESS"
                TrustPetya = $true
            }
        }
        "C" {
            return [pscustomobject]@{
                PlayerId = "petya"
                OfficeRole = "PHONE_C_MOVING_TARGET"
                TrustPetya = $false
            }
        }
        "Operator" {
            return [pscustomobject]@{
                PlayerId = "local-you"
                OfficeRole = "PHONE_OPERATOR"
                TrustPetya = $true
            }
        }
    }
}

function Invoke-Adb([string[]]$Arguments) {
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & adb @Arguments 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    if ($output) {
        $output | Write-Host
    }

    $text = ($output | Out-String)
    if ($exitCode -ne 0 -or $text -match "(?m)\bFailure \[|^adb: error:|^run-as:|^cp: ") {
        throw "adb failed: adb $($Arguments -join ' ')"
    }
}

$config = Get-RoleConfig $Role
$resolvedApk = Resolve-Path $ApkPath

if (-not $SkipInstall) {
    Invoke-Adb @("-s", $Serial, "install", "-r", $resolvedApk.Path)
}

Invoke-Adb @("-s", $Serial, "shell", "am", "force-stop", "net.afterday.compas")

$prefsDir = Join-Path "artifacts" "device-prefs"
New-Item -ItemType Directory -Force $prefsDir | Out-Null
$prefsPath = Join-Path $prefsDir "iff-$($Role.ToLowerInvariant())-$Serial.xml"

$trustLine = if ($config.TrustPetya) {
    '    <boolean name="trusted_player_petya" value="true" />'
} else {
    '    <boolean name="trusted_player_petya" value="false" />'
}

$xml = @(
    "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>",
    "<map>",
    "    <string name=""local_device_player_id"">$($config.PlayerId)</string>",
    '    <boolean name="field_radio_enabled" value="true" />',
    $trustLine,
    "</map>"
)

[System.IO.File]::WriteAllLines($prefsPath, $xml, [System.Text.UTF8Encoding]::new($false))

$remotePrefs = "/data/local/tmp/iff-$($Role.ToLowerInvariant())-$Serial.xml"
Invoke-Adb @("-s", $Serial, "push", $prefsPath, $remotePrefs)
try {
    Invoke-Adb @("-s", $Serial, "shell", "run-as", "net.afterday.compas", "mkdir", "shared_prefs")
} catch {
    # Fresh installs need the directory; existing installs can report it already exists.
}
Invoke-Adb @("-s", $Serial, "shell", "run-as", "net.afterday.compas", "cp", $remotePrefs, "shared_prefs/iff.xml")
Invoke-Adb @("-s", $Serial, "shell", "run-as", "net.afterday.compas", "chmod", "660", "shared_prefs/iff.xml")
Invoke-Adb @("-s", $Serial, "shell", "am", "start", "-n", "net.afterday.compas/.MainActivity")

Write-Host "Prepared $Serial as role $Role / $($config.OfficeRole) / localDevicePlayerId=$($config.PlayerId)."
Write-Host "Open IFF and verify: THIS DEVICE should match the player, OFFICE ROLE should match $($config.OfficeRole)."
