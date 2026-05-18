# Compass APK Project Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Recover `compassv33.apk` into a buildable Android project that can be migrated to modern Android API levels.

**Architecture:** Keep the APK-derived artifacts separate from handwritten migration code. First decode the APK into resources/smali and decompile Java for analysis, then create a modern Android project shell and port code/resources in controlled slices.

**Tech Stack:** Android SDK 36/37 build-tools, Java 21 from Android Studio, Apktool 3.0.2, JADX 1.5.x, Gradle 9.3.1 local wrapper distribution, Android Gradle Plugin once available.

---

### Task 1: Tooling And Decode

**Files:**
- Create: `tools/README.md`
- Create: `tools/apktool-cli-3.0.2-all.jar`
- Create: `tools/jadx-1.5.5/`
- Create: `recovered/apktool/`
- Create: `recovered/jadx/`
- Create: `scripts/rebuild-baseline.ps1`

- [x] **Step 1: Download local APK recovery tools**

Run:

```powershell
New-Item -ItemType Directory -Force -Path .\tools | Out-Null
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/apktool/apktool-cli/3.0.2/apktool-cli-3.0.2-all.jar" -OutFile ".\tools\apktool-cli-3.0.2-all.jar"
```

Expected: `tools/apktool-cli-3.0.2-all.jar` exists and is around 15 MB.

- [x] **Step 2: Decode resources and smali**

Run:

```powershell
& "C:\Program Files\Android\openjdk\jdk-21.0.8\bin\java.exe" -jar .\tools\apktool-cli-3.0.2-all.jar d .\compassv33.apk -o .\recovered\apktool --force
```

Expected: `recovered/apktool/AndroidManifest.xml`, `recovered/apktool/res/`, and `recovered/apktool/smali/` exist.

- [x] **Step 3: Decompile Java-like source for reading**

Run:

```powershell
.\tools\jadx-1.5.5\bin\jadx.bat -d .\recovered\jadx .\compassv33.apk
```

Expected: `recovered/jadx/sources/net/afterday/compas/MainActivity.java` exists.

### Task 2: Buildable Project Skeleton

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/`
- Create: `app/src/main/assets/`

- [x] **Step 1: Create a minimal Android app project**

Run:

```powershell
New-Item -ItemType Directory -Force -Path .\app\src\main | Out-Null
```

Expected: Gradle project files and `app/src/main` exist.

- [x] **Step 2: Copy decoded resources and assets**

Run:

```powershell
Copy-Item -Recurse -Force .\recovered\apktool\res .\app\src\main\
Copy-Item -Recurse -Force .\recovered\apktool\assets .\app\src\main\
```

Expected: original layouts, drawables, raw sounds, fonts, and app assets are present under `app/src/main`.

### Task 3: Source Recovery Slice

**Files:**
- Create: `app/src/main/java/net/afterday/compas/`
- Create: `docs/recovery/class-map.md`

- [x] **Step 1: Generate class inventory**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\openjdk\jdk-21.0.8'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
& "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin\apkanalyzer.bat" dex packages .\compassv33.apk | Select-String -Pattern '^P d .*net\.afterday|^C d .*net\.afterday' | Set-Content .\docs\recovery\class-map.md
```

Expected: `docs/recovery/class-map.md` lists app classes and packages.

- [ ] **Step 2: Port low-risk model and persistency classes first**

Start with:

```text
net.afterday.compas.core.*
net.afterday.compas.persistency.*
net.afterday.compas.serialization.*
net.afterday.compas.settings.*
net.afterday.compas.util.*
```

Expected: non-Android-heavy domain code compiles before sensor/UI code is migrated.

### Task 4: Modern Android Migration

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/net/afterday/compas/sensors/`
- Modify: `app/src/main/java/net/afterday/compas/LocalMainService.java`

- [x] **Step 1: Raise SDK targeting**

Use:

```kotlin
android {
    namespace = "net.afterday.compas"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.afterday.compas"
        minSdk = 23
        targetSdk = 35
        versionCode = 1814
        versionName = "1814-recovered"
    }
}
```

Expected: project targets a Play-compatible modern API level.

- [x] **Step 2: Replace legacy permissions**

Manifest must include modern Bluetooth and Wi-Fi declarations:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" />
```

Expected: runtime permission prompts are implemented before sensor calls execute.

### Task 5: Verification

**Files:**
- Create: `docs/recovery/verification.md`

- [ ] **Step 1: Build debug APK**

Run:

```powershell
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" :app:assembleDebug
```

Expected: `app/build/outputs/apk/debug/app-debug.apk` exists.

- [ ] **Step 2: Install and smoke test on emulator/device**

Run only after the user confirms a device/emulator is available:

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

Expected: app launches, requests permissions, opens the PDA UI, plays sounds, and scanner screen opens.
