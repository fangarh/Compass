# Phase 12 Plan: IFF Confidence Model

## Goal

Make IFF decisions visible as separate confidence layers:

```text
identity -> proximity -> position -> direction
```

The user should be able to see why a participant is trusted locally, why
proximity is or is not confirmed, and which layers are still unavailable.

## Scope

- Add `IffConfidence` as a local decision model.
- Score identity:
  - local self;
  - roster only;
  - roster plus fresh radio claim;
  - no crypto identity yet.
- Score proximity:
  - fresh strong/medium/weak RSSI;
  - stale radio;
  - unknown;
  - local approach button remains local-only.
- Keep position as `UNKNOWN 0%`.
- Keep direction as `UNKNOWN 0%`.
- Update IFF UI:
  - `КОНТАКТ` shows the four confidence layers;
  - `КОМАНДА` shows identity/proximity percentages per player;
  - `КАРТА` states position/direction are still unavailable.

## Files

- `app/src/main/java/net/afterday/compas/iff/IffConfidence.java`
- `app/src/main/java/net/afterday/compas/IffActivity.java`
- `.planning/ROADMAP.md`
- `.planning/STATE.md`
- `stalker/Решения/2026-05-19 MVP IFF Roadmap.md`

## Verification

- Build debug APK with `:app:assembleDebug`.
- Install on a physical phone.
- Verify IFF opens and shows `CONFIDENCE`.
- Verify absent beacon keeps proximity low/unknown.
- If a known `COMPASS_IFF_*` hotspot is available, verify the selected player
  gets higher proximity confidence while position/direction remain `UNKNOWN`.

## Out Of Scope

- No crypto identity.
- No GPS position confidence.
- No direction/witness geometry.
- No radio calibration per phone model.
- No gameplay influence changes.
