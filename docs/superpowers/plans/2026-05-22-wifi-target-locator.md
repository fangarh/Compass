# Wi-Fi Target Locator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a field MVP that estimates a known Wi-Fi target's distance bucket and clock direction from two anchor phones' RSSI measurements.

**Architecture:** Add a pure Java estimator that consumes left/right anchor RSSI windows and returns `5/10/15/20/25+ m` plus clock direction. Then log target RSSI observations from Wi-Fi scans and surface the estimator output in diagnostics, so field data can validate the model before UI polish.

**Tech Stack:** Android Java, `WifiManager` scan results, existing `FieldDiagnosticLog`, standalone PowerShell/Javac tests.

---

### Task 1: Pure Estimator

**Files:**
- Create: `app/src/main/java/net/afterday/compas/iff/IffWifiTargetLocator.java`
- Create: `scripts/test-data/iff-wifi-target-locator/IffWifiTargetLocatorTest.java`
- Create: `scripts/test-iff-wifi-target-locator.ps1`

- [ ] Write failing tests for center/left/right/no-data cases.
- [ ] Implement distance bucket from mean RSSI and clock direction from right-minus-left RSSI delta.
- [ ] Run `scripts/test-iff-wifi-target-locator.ps1`.

### Task 2: Runtime Target Scan Logging

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/iff/IffForegroundRadioService.java`

- [ ] Detect configured target SSID candidates in each Wi-Fi scan.
- [ ] Log `event=wifi_target_observation` with `localDevicePlayerId`, `targetPlayerId`, `ssid`, `bssid`, `rssi`, `frequency`.
- [ ] Include a compact `wifiTargetStatus` field in `auto_field_check`.

### Task 3: Build And Device Smoke

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] Bump version name/code.
- [ ] Run estimator test and `gradle :app:assembleDebug`.
- [ ] Install on connected phones and confirm no crash-buffer output.
