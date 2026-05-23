# BLE GPS Witness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Carry remote GPS witness data over the existing BLE field radio so field distance and bearing do not depend on Wi-Fi UDP broadcast.

**Architecture:** Keep BLE v1 player-only advertisements as fallback and add a compact BLE v2 manufacturer payload with optional GPS fields. The scanner accepts both payloads; v2 updates the existing `IffRemoteWitnessStore`, allowing `IffForegroundRadioService` to compute `gpsDistanceM` and `gpsBearingDeg` from the already implemented GPS pair logic.

**Tech Stack:** Android BLE advertiser/scanner, Java payload codec, existing Java harness scripts, Gradle Android debug build.

---

### Task 1: BLE GPS Payload Codec

**Files:**
- Create: `app/src/main/java/net/afterday/compas/iff/IffBlePayload.java`
- Create: `scripts/test-data/iff-ble-payload/IffBlePayloadTest.java`
- Create: `scripts/test-iff-ble-payload.ps1`

- [ ] **Step 1: Write the failing test**

Add a Java harness test that expects v1 compatibility, v2 GPS roundtrip, and invalid payload rejection.

- [ ] **Step 2: Run test to verify it fails**

Run: `powershell -ExecutionPolicy Bypass -File scripts\test-iff-ble-payload.ps1`

Expected: compilation failure because `IffBlePayload` does not exist.

- [ ] **Step 3: Write minimal implementation**

Implement `IffBlePayload` as a pure Java codec with a 4-byte v1 payload and a compact v2 payload containing player code, lat/lon E7, accuracy meters, and age seconds.

- [ ] **Step 4: Run test to verify it passes**

Run: `powershell -ExecutionPolicy Bypass -File scripts\test-iff-ble-payload.ps1`

Expected: `IFF BLE payload test passed.`

### Task 2: BLE Radio Integration

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/iff/IffBleFieldRadio.java`
- Modify: `app/src/main/java/net/afterday/compas/iff/IffForegroundRadioService.java`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Expose latest local GPS to BLE radio**

Add `IffBleFieldRadio.updateLocalGps(Location)` and call it from `IffForegroundRadioService` after reading best location.

- [ ] **Step 2: Advertise v2 when GPS is available**

Build v2 manufacturer data when local GPS is fresh enough; otherwise keep v1 payload.

- [ ] **Step 3: Parse v2 on scan**

Use `IffBlePayload.parse`, update RSSI witness as before, and write a GPS-bearing `IffRemoteWitnessReport` to `IffRemoteWitnessStore` when GPS exists.

- [ ] **Step 4: Add diagnostics**

Log `contract=ble-iff-v2` and `gps=true/false` in `ble_field_radio_rx`; add `remoteGpsSource=ble/none` to `auto_field_check`.

### Task 3: Verification and Install

**Files:**
- No source ownership beyond Task 1 and Task 2.

- [ ] **Step 1: Run focused Java tests**

Run:
- `powershell -ExecutionPolicy Bypass -File scripts\test-iff-ble-payload.ps1`
- `powershell -ExecutionPolicy Bypass -File scripts\test-iff-auto-field-check.ps1`
- `powershell -ExecutionPolicy Bypass -File scripts\test-iff-remote-witness-frame.ps1`

- [ ] **Step 2: Build Android APK**

Run: `gradle :app:assembleDebug`

- [ ] **Step 3: Install on connected phones**

Run `adb devices`, then install the debug APK on connected devices and verify the package version.

- [ ] **Step 4: Smoke check**

Launch `net.afterday.compas/.IffActivity`, wait for fresh logs, and confirm `ble_field_radio_rx gps=true` or, if GPS is stale indoors, that v2 fallback does not regress BLE RSSI.
