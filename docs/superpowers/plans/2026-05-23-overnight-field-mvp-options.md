# Overnight Field MVP Options Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prepare concrete morning-ready MVP options for determining approximate target distance and clock direction with only phones.

**Architecture:** Keep the current field radio stack as the base, then add a small decision layer that can report a user-facing "where is target" estimate from the best available evidence. Prefer evidence already available on phones: Wi-Fi SSID RSSI when the target exposes `COMPASS_IFF_ZHENYA`, BLE/RSSI witness windows when Wi-Fi target SSID is absent, and GPS pair data when both phones have fresh fixes.

**Tech Stack:** Android Java, existing `IffForegroundRadioService`, `IffRadioWitnessStore`, `IffRemoteWitnessStore`, Wi-Fi Direct service discovery, BLE witness reports, standalone PowerShell/Javac tests, `adb` device smoke tests.

---

### Task 1: Document MVP Variants And Field Assumptions

**Files:**
- Create: `stalker/AI/Handoffs/2026-05-23 overnight field MVP handoff.md`

- [ ] Record the permission model for overnight device work.
- [ ] Record three implementation variants: Wi-Fi SSID target locator, BLE/RSSI fallback locator, GPS assisted locator.
- [ ] Mark the practical field requirement: no more than 2 seconds between local measurement ticks, but Wi-Fi Direct peer exchange may be slower.

### Task 2: Add A Unified Field Locator Snapshot

**Files:**
- Create: `app/src/main/java/net/afterday/compas/iff/IffFieldLocatorSnapshot.java`
- Create: `scripts/test-data/iff-field-locator/IffFieldLocatorSnapshotTest.java`
- Create: `scripts/test-iff-field-locator.ps1`

- [ ] Write failing tests for Wi-Fi target estimate priority.
- [ ] Write failing tests for BLE fallback estimate when Wi-Fi target is insufficient.
- [ ] Write failing tests for GPS assisted status when radio estimate is unavailable.
- [ ] Implement a pure Java snapshot formatter that returns source, distance bucket, clock direction, confidence, and reason.
- [ ] Run `scripts/test-iff-field-locator.ps1`.

### Task 3: Wire Locator Into Diagnostics

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/iff/IffForegroundRadioService.java`

- [ ] Build `fieldLocatorStatus` from Wi-Fi target status first.
- [ ] Fall back to existing radio distance trend when Wi-Fi target status is `INSUFFICIENT_DATA`.
- [ ] Include GPS status in the reason, without claiming GPS direction when the fix is stale/outlier.
- [ ] Log `fieldLocatorStatus="..."` in every `auto_field_check`.

### Task 4: Morning Device Smoke

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `artifacts/field-mvp-20260523-*`

- [ ] Bump app version after code changes.
- [ ] Run all targeted tests.
- [ ] Run `gradle :app:assembleDebug`.
- [ ] Install on all connected phones.
- [ ] Launch IFF screen on connected phones and pull diagnostics.
- [ ] Summarize what works without user movement and what still requires outdoor field validation.
