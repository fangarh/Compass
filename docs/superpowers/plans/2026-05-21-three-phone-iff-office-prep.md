# Three Phone IFF Office Prep Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prepare the current IFF build and documentation for the office three-phone BLE witness test.

**Architecture:** Keep BLE radio behavior unchanged. Add explicit office-role metadata around the existing local device identity so field logs can distinguish witness A, witness B, and moving target C without guessing from physical devices.

**Tech Stack:** Android Java, PowerShell field-log analyzer, Obsidian Markdown under `stalker/`.

---

### Task 1: Office Role Labels

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/IffActivity.java`

- [ ] Add a helper that maps local roster identity to office test role:

```java
private String officeTestRole(IffPlayer player) {
    if (player == null) {
        return "UNASSIGNED";
    }
    if ("vasya".equals(player.playerId)) {
        return "PHONE_A_WITNESS";
    }
    if ("zhenya".equals(player.playerId)) {
        return "PHONE_B_WITNESS";
    }
    if ("petya".equals(player.playerId)) {
        return "PHONE_C_MOVING_TARGET";
    }
    if ("local-you".equals(player.playerId)) {
        return "PHONE_OPERATOR";
    }
    return "UNASSIGNED";
}
```

- [ ] Show the role in team/contact/map status near `THIS DEVICE`.

- [ ] Add `officeRole=<role>` to `IFF_DIAG event=field_check`.

- [ ] Add `officeRole=<role>` to `IFF_DIAG event=device_identity_selected`.

### Task 2: Analyzer Export

**Files:**
- Modify: `scripts/analyze-field-logs.ps1`

- [ ] Parse `officeRole` from `field_check` messages into `$iffFieldChecks`.

- [ ] Add `OfficeRole` to `iff-field-checks.csv`.

- [ ] Add office role to `iff-field-check-summary.csv` using label counts.

- [ ] Include office role in the `IFF Field Checks` Markdown tables.

### Task 3: Office Test Checklist

**Files:**
- Create: `stalker/Проверки/2026-05-21 Three Phone IFF Office Test Plan.md`
- Modify: `stalker/Компас - индекс.md`

- [ ] Document role assignment: `Вася = Phone A witness`, `Женя = Phone B witness`, `Петя = Phone C moving target`.

- [ ] Document the exact run steps: A/B stationary at 7 m, C near A, C middle, C near B, pocket/body shield, C off/away, repeat at 10-14 m if office space allows.

- [ ] Document expected evidence: RSSI stronger near closer witness, stale/unknown transitions, no exact position/direction claim.

- [ ] Add the new test plan to the central Obsidian index.

### Task 4: Verification

**Files:**
- Build/test commands only.

- [ ] Run the analyzer smoke test:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\test-analyze-field-logs.ps1
```

- [ ] Run Android build:

```powershell
npm run build
```

If Android build is not wired to npm in this repo, run the Gradle wrapper or local Gradle command already documented in `stalker/AI/Состояние проекта.md`.
