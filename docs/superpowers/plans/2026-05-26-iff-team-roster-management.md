# IFF Team Roster Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build local add/remove team management for the IFF screen.

**Architecture:** Persist confirmed members and removed IDs in a small pure Java roster store. `IffActivity` renders confirmed members by default, renders discovered candidates only in explicit search mode, and filters map participants to confirmed team IDs.

**Tech Stack:** Android Java, SharedPreferences, existing IFF radio stores, PowerShell JVM tests.

---

### Task 1: Roster Store

**Files:**
- Create: `app/src/main/java/net/afterday/compas/iff/IffTeamRosterStore.java`
- Create: `scripts/test-data/iff-team-roster-store/IffTeamRosterStoreTest.java`
- Create: `scripts/test-iff-team-roster-store.ps1`

- [ ] Implement a pure Java roster store with default entries, serialize/deserialize helpers, removed-ID tracking, explicit `addOrRestore`, and `remove`.
- [ ] Test seed, removal, explicit restore, serialization, and invalid input.

### Task 2: Discovery APIs

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/iff/IffRadioWitnessStore.java`
- Modify: `app/src/main/java/net/afterday/compas/iff/IffForegroundRadioService.java`
- Modify: `app/src/main/java/net/afterday/compas/iff/IffParticipantMapModel.java`

- [ ] Expose snapshot copies of current witnesses and participant states.
- [ ] Add participant-map filtering by allowed player IDs.

### Task 3: IFF UI Integration

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/IffActivity.java`

- [ ] Load/save roster and removed IDs from existing `iff` preferences.
- [ ] Add `SEARCH` toggle and discovered candidate buttons to `TEAM`.
- [ ] Add `REMOVE FROM TEAM` on non-local contact cards.
- [ ] Prevent removing the current `THIS DEVICE`.
- [ ] Filter team, contacts, counters, witness quorum, and map to confirmed team IDs.

### Task 4: Verification

**Files:**
- Run: `scripts/test-iff-team-roster-store.ps1`
- Run existing IFF unit scripts touched by the change.
- Run `:app:assembleDebug`.

- [ ] Confirm default roster still starts with `local-you`, `petya`, `vasya`, `zhenya`.
- [ ] Confirm removed members are not rendered in normal `TEAM` and map flow.
- [ ] Confirm explicit search can re-add a removed member.
