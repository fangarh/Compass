# Field Team APK Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a near-term `standardDebug` APK that lets up to three real phones form a field team and test IFF/map readiness without PC-side setup.

**Architecture:** Keep the existing internal player IDs as stable protocol roles, but hide them from users behind editable display names. Add an explicit in-app field setup surface in `IffActivity` so each phone can select its role, turn on field radio, and see readiness directly on screen.

**Tech Stack:** Android Java app, Gradle Kotlin DSL, support libraries, BLE/GPS/Wi-Fi Direct IFF services already present in the repository.

---

## Current Facts

- Field APK should be `standardDebug`, not `hiddenMainActions`, so IFF/LOG are visible from the main screen.
- Field test uses up to three physical phones total.
- Existing display-name input stays in use; users enter their own names there.
- Internal role IDs remain fixed:
  - Role A: `vasya`
  - Role B: `zhenya`
  - Role C: `petya`
- Field UI must show display names, not technical IDs, wherever a normal user is expected to read names.
- No log export is required for this pass. Diagnostics stay on screen in TEAM/LOG.
- No PC/adb preparation should be needed in the field after installing the APK, except Android permissions, developer mode, and Wi-Fi scan throttling unlock.

## Files

- Modify: `app/src/main/java/net/afterday/compas/IffActivity.java`
  - Add explicit field setup controls.
  - Apply field role selection.
  - Limit the field roster to A/B/C.
  - Render readiness and participant names on screen.
- Modify: `app/src/main/java/net/afterday/compas/MainActivity.java`
  - Request `BLUETOOTH_ADVERTISE` on Android 12+.
- Create: `app/src/main/java/net/afterday/compas/iff/IffFieldTeamProfile.java`
  - Pure helper for field role definitions and roster reset behavior.
- Create: `scripts/test-iff-field-team-profile.ps1`
  - Compile/run pure Java assertions for the helper.
- Reuse existing tests/scripts:
  - `scripts/test-iff-team-roster-store.ps1`
  - `scripts/test-iff-participant-display-names.ps1`
  - `scripts/test-iff-field-locator.ps1`
  - `scripts/test-main-action-flavors.ps1`

## Task 1: Add Pure Field Team Profile

**Files:**
- Create: `app/src/main/java/net/afterday/compas/iff/IffFieldTeamProfile.java`
- Create: `scripts/test-iff-field-team-profile.ps1`

- [ ] Add `IffFieldTeamProfile` with three roles only: A/`vasya`, B/`zhenya`, C/`petya`.
- [ ] Expose helper methods:
  - `roles()`
  - `isFieldPlayerId(String playerId)`
  - `roleForPlayerId(String playerId)`
  - `defaultFieldEntries()`
  - `fieldEntriesPreservingNames(List<IffTeamRosterStore.Entry> current)`
- [ ] Preserve display names from existing roster entries when resetting field team.
- [ ] Do not include `local-you` in field roster defaults.
- [ ] Add the PowerShell test script with assertions for role count, role IDs, max three entries, and name preservation.
- [ ] Run:

```powershell
.\scripts\test-iff-field-team-profile.ps1
```

Expected: all assertions pass.

## Task 2: Add In-App Role Selection

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/IffActivity.java`

- [ ] In the TEAM tab, add a compact "FIELD SETUP" block before roster buttons.
- [ ] Add role buttons `A`, `B`, `C`; selecting a role must:
  - set `local_device_player_id` to the role player ID;
  - ensure the field roster contains only A/B/C;
  - preserve existing display names for A/B/C;
  - set `field_radio_enabled=true`;
  - clear approach state;
  - restart `IffForegroundRadioService.start(this, localDevicePlayerId, localDevicePlayer().displayName)`;
  - refresh the current UI.
- [ ] Add `RESET FIELD TEAM`; it must restore exactly A/B/C and keep known display names where available.
- [ ] Keep the existing long-press identity behavior as a fallback, but field testers should not need it.

## Task 3: Display Names Everywhere Field Users Read Names

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/IffActivity.java`

- [ ] Audit TEAM/MAP/LOG strings that show participant identity to users.
- [ ] Use existing `displayNameFor(IffPlayer player)` for visible participant labels when possible.
- [ ] Keep technical IDs only in diagnostic detail lines where they help debugging.
- [ ] Ensure discovered candidates use received `IffParticipantState.displayName` when available.
- [ ] Verify the map label for the moving target does not hardcode a stale user-facing name if the current target is `petya`.

## Task 4: On-Screen Field Readiness

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/IffActivity.java`

- [ ] Add a concise readiness section in TEAM and/or LOG with:
  - local role and local display name;
  - field radio enabled/on state;
  - BLE advertise/scan status from existing compact status methods;
  - visible team members with display names;
  - GPS/map readiness reason when map points are hidden.
- [ ] Reuse existing status providers:
  - `IffForegroundRadioService.compactStatus()`
  - `IffBleFieldRadio.compactStatus()`
  - `IffBleFieldRadio.lifecycleStatus()`
  - `IffForegroundRadioService.participantMapSnapshot(localDevicePlayerId)`
- [ ] Do not add log sharing or FileProvider work in this pass.

## Task 5: Fix Runtime Permissions for Android 12+

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/MainActivity.java`
- Optionally modify: `app/src/main/java/net/afterday/compas/IffActivity.java`

- [ ] Add `Manifest.permission.BLUETOOTH_ADVERTISE` to Android 12+ startup permission requests.
- [ ] If IFF can be opened without going through main startup permission flow, add an IFF-local permission retry button or request path for missing IFF permissions.
- [ ] Keep existing requests for location, camera, Bluetooth scan/connect, nearby Wi-Fi devices, and notifications.

## Task 6: Verification Build and Device Smoke Test

**Files:**
- No planned source edits in this task unless a verification failure identifies a bug.

- [ ] Run pure Java scripts:

```powershell
.\scripts\test-iff-team-roster-store.ps1
.\scripts\test-iff-participant-display-names.ps1
.\scripts\test-iff-field-locator.ps1
.\scripts\test-iff-field-team-profile.ps1
.\scripts\test-main-action-flavors.ps1
```

- [ ] Build the APK:

```powershell
.\gradlew.bat :app:assembleStandardDebug
```

Expected APK:

```text
app/build/outputs/apk/standard/debug/app-standard-debug.apk
```

- [ ] On real phones, verify a fresh install with no adb-prepared preferences:
  - app opens with visible IFF button;
  - required runtime permissions are requestable from the app;
  - each phone can choose A/B/C;
  - each phone can enter a display name;
  - team size stays at no more than three;
  - phones see one another in TEAM/LOG by display name;
  - MAP/LOG shows a concrete reason if GPS or locator readiness is missing.

## Acceptance Criteria

- A tester can install the APK, grant permissions, choose A/B/C, enter a name, and start field testing without PC commands.
- The app never requires `prepare-iff-device.ps1` for normal field setup.
- TEAM and LOG provide enough on-screen status to explain why phones do or do not appear on the map.
- The final APK is `standardDebug` and includes visible IFF access from the main screen.

## Home Continuation Prompt

Use this prompt after cloning or downloading the repository at home:

```text
I am continuing the Compass Android field APK work. Please read docs/superpowers/plans/2026-07-01-field-team-apk.md first, then inspect git status and the current IFF-related code before editing. Implement the plan for a standardDebug APK that lets up to three phones form a field team without adb preparation: roles A=vasya, B=zhenya, C=petya internally, user-entered display names on screen, visible readiness in TEAM/LOG, no log export. Preserve unrelated worktree changes. Build and verify with the listed scripts and .\gradlew.bat :app:assembleStandardDebug. Answer in Russian.
```
