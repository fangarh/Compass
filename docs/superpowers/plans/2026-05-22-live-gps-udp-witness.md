# Live GPS UDP Witness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add live GPS distance and bearing evidence to the existing UDP witness channel.

**Architecture:** Keep the current UDP witness transport and extend its frame with optional GPS fields. Parse/build is isolated in a pure Java codec so protocol behavior is testable without Android. Foreground service broadcasts this phone's GPS fix and computes `gpsDistanceM/gpsBearingDeg` from local GPS plus the freshest remote GPS report.

**Tech Stack:** Android Java service, UDP datagrams, existing PowerShell/Java test harness, Gradle Android build.

---

### Task 1: Pure UDP Frame Codec

**Files:**
- Create: `app/src/main/java/net/afterday/compas/iff/IffRemoteWitnessFrame.java`
- Create: `scripts/test-data/iff-remote-witness-frame/IffRemoteWitnessFrameTest.java`
- Create: `scripts/test-iff-remote-witness-frame.ps1`

- [ ] Write a failing test proving a frame with `gpsLatE7`, `gpsLonE7`, `gpsAccuracyM`, and `gpsAgeMs` round-trips.
- [ ] Run `powershell -ExecutionPolicy Bypass -File scripts\test-iff-remote-witness-frame.ps1`; expect compile failure because the codec does not exist.
- [ ] Implement the minimal codec and run the test green.

### Task 2: Wire GPS Into Remote Witness Reports

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/iff/IffRemoteWitnessReport.java`
- Modify: `app/src/main/java/net/afterday/compas/iff/IffRemoteWitnessStore.java`
- Modify: `app/src/main/java/net/afterday/compas/iff/IffUdpWitnessTransport.java`

- [ ] Add optional GPS fields to `IffRemoteWitnessReport`.
- [ ] Use `IffRemoteWitnessFrame` in UDP build/parse.
- [ ] Add store lookup for the freshest valid GPS report for a target player.

### Task 3: Foreground Service Live GPS Snapshot

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/iff/IffForegroundRadioService.java`
- Modify: `app/build.gradle.kts`

- [ ] Broadcast this phone's local player GPS on every 2s auto snapshot.
- [ ] Compute `gpsDistanceM/gpsBearingDeg` from local fix to remote target fix when available.
- [ ] Bump version to `1827-live-gps-udp-witness`.
- [ ] Run `scripts\test-iff-remote-witness-frame.ps1`, `scripts\test-iff-auto-field-check.ps1`, and `gradle :app:assembleDebug`.
