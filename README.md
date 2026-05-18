# Compass Recovered

Recovered Android project for `PDA Compass V3.1` (`net.afterday.compas`).

## What is included

- Modern Android Gradle project under `app/`.
- Recovery notes and verification docs under `docs/`.
- Utility scripts under `scripts/`.
- Original recovery input APK: `compassv33.apk`.

Generated build outputs, decompiler work folders, downloaded tools, and handoff APKs are intentionally ignored by git. See `docs/recovery/` and `docs/user-guide/` for current project status and user-facing documentation.

## Build

This project currently expects a local Android SDK and Gradle installation.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
gradle :app:assembleDebug
```

Current target package:

```text
net.afterday.compas
```
