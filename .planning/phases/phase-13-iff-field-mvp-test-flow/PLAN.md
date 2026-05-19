# Phase 13 Plan: IFF Field MVP Test Flow

## Goal

Make the current IFF MVP usable for repeated field checks:

```text
select contact -> inspect confidence -> record current verdict -> review log
```

The recorded verdict is a field-test artifact, not a gameplay network event.

## Scope

- Add a `ЗАПИСАТЬ` button to the IFF screen.
- Log a structured `IFF_DIAG event=field_check`.
- Include in the log:
  - selected player id/name;
  - identity confidence label/score;
  - proximity confidence label/score;
  - position confidence label/score;
  - direction confidence label/score;
  - current radio witness if present;
  - local approach state.
- Show the last recorded check summary in the IFF UI.

## Files

- `app/src/main/java/net/afterday/compas/IffActivity.java`
- `app/src/main/res/layout/iff_activity.xml`
- `app/src/main/res/values/ids.xml`
- `.planning/ROADMAP.md`
- `.planning/STATE.md`
- `stalker/Решения/2026-05-19 MVP IFF Roadmap.md`

## Verification

- Build debug APK.
- Install on OnePlus.
- Open `IFF`.
- Select `Петя`.
- Tap `ЗАПИСАТЬ`.
- Confirm the UI shows the last recorded field-check summary.
- Confirm logcat or diagnostics contain `IFF_DIAG event=field_check`.

## Out Of Scope

- No automatic sync.
- No crypto identity.
- No GPS position confidence.
- No direction calculation.
- No new radio protocol.
