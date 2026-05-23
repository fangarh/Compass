# Field MVP UI Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make field proximity tolerate wall-shadow one-sided BLE evidence, and keep game screens free of mock/debug data by moving diagnostics to log screens.

**Architecture:** Keep field diagnostics in logs and a dedicated IFF log tab. Main game UI loses the embedded log list and gains a small log button. IFF contact/team/map tabs show only player-facing state; simulation and transport debug controls are removed from the visible game surface.

**Tech Stack:** Android Java, XML layouts, existing Gradle app build, existing PowerShell/Java fixture tests.

---

### Task 1: Wall-Shadow Proximity Verdicts

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/iff/IffOfficeProximityVerdict.java`
- Modify test: `scripts/test-data/iff-auto-field-check/IffAutoFieldCheckSnapshotTest.java`

- [ ] Add failing tests asserting `ONLY_A_VISIBLE` and `ONLY_B_VISIBLE` when the moving target has one usable side and the other side is blocked/missing.
- [ ] Run `powershell -ExecutionPolicy Bypass -File scripts\test-iff-auto-field-check.ps1` and verify it fails on the missing labels.
- [ ] Implement one-sided labels while keeping `INSUFFICIENT_DATA` for no usable side.
- [ ] Re-run the same test and verify it passes.

### Task 2: IFF Game Tabs Minimal, Log Tab Diagnostic

**Files:**
- Modify: `app/src/main/res/layout/iff_activity.xml`
- Modify: `app/src/main/res/values/ids.xml`
- Modify: `app/src/main/java/net/afterday/compas/IffActivity.java`

- [ ] Add a fourth `LOG` tab and remove visible `TX STUB`, `SIM FRESH`, `SIM STALE` buttons from the bottom control row.
- [ ] Keep Contact/Team/Map limited to playable state: title, concise status, roster/map, trust/radio/check controls.
- [ ] Move field radio policy, BLE lifecycle, UDP transport, quorum/confidence detail, and last field check into `renderLog()`.
- [ ] Remove mock wording from Map.

### Task 3: Main Game Log Screen

**Files:**
- Create: `app/src/main/java/net/afterday/compas/GameLogActivity.java`
- Create: `app/src/main/res/layout/activity_game_log.xml`
- Modify: `app/src/main/res/layout-port/activity_main.xml`
- Modify: `app/src/main/res/layout-land/activity_main.xml`
- Modify: `app/src/main/res/values/ids.xml`
- Modify: `app/src/main/java/net/afterday/compas/MainActivity.java`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] Remove embedded `log_background` and `log_list` from both main game layouts.
- [ ] Add a small `LOG` button in main game layouts.
- [ ] Add `GameLogActivity` that renders the existing `Logger` stream via `SmallLogListAdapter`.
- [ ] Wire the main `LOG` button to open `GameLogActivity`.

### Task 4: Verification

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] Bump debug version to a new field UI build.
- [ ] Run `powershell -ExecutionPolicy Bypass -File scripts\test-iff-auto-field-check.ps1`.
- [ ] Run `powershell -ExecutionPolicy Bypass -File scripts\test-analyze-field-logs.ps1`.
- [ ] Run `gradle :app:assembleDebug`.
- [ ] If devices are connected, install APK and smoke-launch `MainActivity`, `GameLogActivity`, and `IffActivity`.
