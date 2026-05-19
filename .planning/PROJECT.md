# Compass

## Source Of Truth

Primary project context lives in the Obsidian vault under `stalker/`.

Entry point:

- `stalker/Компас - индекс.md`

Key context documents:

- `stalker/AI/Состояние проекта.md`
- `stalker/AI/Карта кодовой базы.md`
- `stalker/Решения/2026-05-18 Радиодетекция Wi-Fi BLE.md`
- `stalker/Решения/2026-05-18 Obsidian как источник истины.md`

## Project Summary

Compass is a recovered Android Gradle project for `PDA Compass V3.1`, package
`net.afterday.compas`.

The current engineering goal is to preserve the recovered application, stabilize
it on modern Android, and support field validation of radio behavior. Wi-Fi
remains the required near-term radio detection layer because the target hardware
and customer workflow depend on Wi-Fi modules. BLE remains a deferred direction.

## Current Baseline

- Android module: `:app`
- Application id: `net.afterday.compas`
- compileSdk: `36`
- targetSdk: `35`
- minSdk: `23`
- Debug version name: `1816-default-5s`
- Physical field test device: Samsung SM-S908B, adb serial `R3CT20C8A8N`
- Emulator is available, but cannot validate real Wi-Fi radio behavior.

## Constraints

- The source was recovered from APK and still contains decompiled-code artifacts.
- Avoid broad refactors unless needed for a concrete phase goal.
- Preserve current gameplay behavior while adding diagnostics.
- Treat real Wi-Fi behavior as device-dependent and validate on the physical phone.
- Keep `stalker/` synchronized when decisions or field findings change.

## First Increment

Field Diagnostic Log for Wi-Fi/radio tests on the physical phone.

The increment must write a diagnostic file on the device so field logs can be
pulled with ADB after tests and analyzed offline.
