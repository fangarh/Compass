# Recovery Verification

## APK Decode

- Apktool 3.0.2 decoded `compassv33.apk` successfully into `recovered/apktool`.
- JADX 1.5.5 exported Java-like source into `recovered/jadx` with 4 decompiler errors.
- App package source copied from `recovered/jadx/sources/net/afterday/compas` into `app/src/main/java/net/afterday/compas`.
- Generated `R.java` and `BuildConfig.java` were removed from `app/src/main/java`.

## Gradle Java Recovery Build

Command:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\openjdk\jdk-21.0.8'
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" :app:assembleDebug
```

Result: failed during Java compilation.

Observed issue classes:

- Decompiled synthetic lambda code references missing `this.f$0` fields in many anonymous classes.
- Decompiled enum switch helper code references missing `$SwitchMap...` arrays.
- `GameImpl` contains a duplicate `controls` field.
- `PlayerImpl` contains unresolved local variables `health` and `rad`.
- Decompiled source imports `android.support.v4.media.TransportMediator`, which exists in older support libraries but not in the newer support dependency used by the first skeleton.
- Decompiled source imports `java8.util.Optional`; the compatibility dependency must be identified or the code should be migrated to `java.util.Optional` where min/target allow it.

Conclusion: JADX output is useful as readable recovery material, but not directly buildable without manual source cleanup. The exact executable baseline should be rebuilt from Apktool smali first, then Java source should be ported package-by-package.

## Modern Gradle Debug Build

Command:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\openjdk\jdk-21.0.8'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" :app:assembleDebug
```

Result: passed.

Created file:

- `app/build/outputs/apk/debug/app-debug.apk`

APK metadata:

```text
package: name='net.afterday.compas' versionCode='1814' versionName='1814-recovered'
sdkVersion:'23'
targetSdkVersion:'35'
application-label:'PDA Compass V3.1'
```

Signature verification:

```text
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
```

Notes:

- Java source now compiles from the recovered Gradle project.
- `res/values` was pruned to app-owned resources; merged AppCompat/Support/ZXing resources extracted from the APK were removed because Gradle dependencies provide them.
- The build still uses old `android.support.*` APIs through compatibility dependencies. A later migration should move this to AndroidX and address runtime permission/foreground-service behavior on real Android 12-15 devices.

## Emulator Smoke Test

AVD:

```text
Name: Compass_API36
Path: D:\Android\avd\Compass_API36.avd
System image: system-images;android-36.1;google_apis_playstore;x86_64
Device profile: pixel_9
```

User environment:

```text
ANDROID_AVD_HOME=D:\Android\avd
```

Runtime fixes applied after the first Android 16 emulator launch:

- Added `PendingIntent.FLAG_IMMUTABLE` to foreground-service notification intents in `LocalMainService`.
- Added a notification channel for the foreground-service notification.
- Removed the manifest-declared `WifiImpl$WifiReceiver`; it is a non-static inner receiver and Android cannot instantiate it from the manifest.

Result:

- Install passed with `adb install -r -d app\build\outputs\apk\debug\app-debug.apk`.
- Runtime permissions were granted with `pm grant`.
- `MainActivity` launched and stayed resumed.
- Foreground service connected: `SERVICE CONNECTED!!!!`.
- Screenshot captured at `docs/recovery/screenshots/compass-launch.png`.

Known runtime warning:

- Android throttles the app's once-per-second Wi-Fi scans: `WifiScanRequestProxy: Scan request from net.afterday.compas throttled`. This does not crash the app, but the Wi-Fi sensor behavior needs redesign for modern Android scan limits.

## Apktool Baseline Rebuild

Command:

```powershell
& 'C:\Program Files\Android\openjdk\jdk-21.0.8\bin\java.exe' -jar .\tools\apktool_3.0.2.jar b .\recovered\apktool -o .\recovered\compassv33-rebuilt-unsigned.apk --frame-path .\tools\apktool-framework
```

Result: passed.

Created files:

- `recovered/compassv33-rebuilt-unsigned.apk`
- `recovered/compassv33-rebuilt-aligned.apk`
- `recovered/compassv33-rebuilt-debugsigned.apk`

Signature verification:

```text
Verifies
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): true
Number of signers: 1
```

The rebuilt APK still has the original manifest values (`versionCode=1813`, `targetSdkVersion=19`). It is a binary recovery baseline, not the migrated modern Android build.

Repeatable command:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\rebuild-baseline.ps1
```
