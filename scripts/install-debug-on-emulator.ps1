param(
    [string]$Serial = "emulator-5554",
    [string]$PackageName = "net.afterday.compas",
    [string]$Apk = "app\build\outputs\apk\debug\app-debug.apk"
)

$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "C:\Program Files\Android\openjdk\jdk-21.0.8"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

adb -s $Serial install -r -d $Apk

$permissions = @(
    "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.BLUETOOTH_SCAN",
    "android.permission.BLUETOOTH_CONNECT",
    "android.permission.NEARBY_WIFI_DEVICES",
    "android.permission.CAMERA",
    "android.permission.POST_NOTIFICATIONS"
)

foreach ($permission in $permissions) {
    adb -s $Serial shell pm grant $PackageName $permission 2>$null
}

adb -s $Serial shell am start -n "$PackageName/.MainActivity"
