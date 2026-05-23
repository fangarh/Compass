# Distance Direction GPS V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add MVP distance classes, movement trend, and GPS quality/status fields to IFF field snapshots without claiming precise indoor meters.

**Architecture:** Keep the model deterministic and local to the IFF layer. RSSI windows produce `DistanceTrendSnapshot`, location diagnostics produce GPS quality in logs, and `auto_field_check` exports fields that the analyzer and UI can display.

**Tech Stack:** Android Java, PowerShell analyzer/tests, existing `IffRadioWitnessStore`, `IffForegroundRadioService`, `IffActivity`, and `scripts/analyze-field-logs.ps1`.

---

### Task 1: Distance And Movement Model

**Files:**
- Create: `app/src/main/java/net/afterday/compas/iff/IffDistanceTrend.java`
- Modify: `scripts/test-data/iff-auto-field-check/IffAutoFieldCheckSnapshotTest.java`
- Modify: `scripts/test-iff-auto-field-check.ps1`

- [ ] Add RED tests for RSSI classes: `VERY_NEAR`, `NEAR`, `MID`, `FAR`, `EDGE`, `LOST`.
- [ ] Add RED tests for movement trend: stronger RSSI means `APPROACHING`, weaker means `LEAVING`, small delta means `STABLE`, insufficient samples means `UNKNOWN`.
- [ ] Implement `IffDistanceTrend.evaluate(...)` using RSSI average, valid sample count, fresh flag, and previous window average.
- [ ] Compile the new class in `scripts/test-iff-auto-field-check.ps1`.

### Task 2: Snapshot Log Contract

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/iff/IffRadioWitnessStore.java`
- Modify: `app/src/main/java/net/afterday/compas/iff/IffForegroundRadioService.java`
- Modify: `scripts/test-data/beacon-field-logs/test-device/field-radio-20260519-130000.log`
- Modify: `scripts/test-analyze-field-logs.ps1`

- [ ] Expose a previous RSSI-window average for each player from `IffRadioWitnessStore`.
- [ ] Add `distanceClass`, `distanceConfidence`, `movementTrend`, `movementConfidence`, and `movementRssiDeltaDb` to `auto_field_check`.
- [ ] Add placeholder GPS contract fields: `gpsStatus`, `gpsAccuracyM`, `gpsDistanceM`, `gpsBearingDeg`.
- [ ] Add analyzer parsing and CSV export for those fields.

### Task 3: UI Surface

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/IffActivity.java`

- [ ] Show distance class and movement trend on `CONTACT`.
- [ ] Show office proximity plus distance/trend/GPS status on `MAP`.
- [ ] Keep detailed values on `LOG`.

### Task 4: Verification

**Commands:**
- `powershell -ExecutionPolicy Bypass -File scripts\test-iff-auto-field-check.ps1`
- `powershell -ExecutionPolicy Bypass -File scripts\test-analyze-field-logs.ps1`
- `powershell -ExecutionPolicy Bypass -File scripts\test-iff-ui-no-mock-controls.ps1`
- `gradle :app:assembleDebug`

- [ ] Run tests after each behavior change.
- [ ] Build APK.
- [ ] Install and run a short field check only after all local verification passes.
