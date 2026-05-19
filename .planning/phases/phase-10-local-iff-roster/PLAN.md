# Phase 10 Plan: Local IFF Roster

## Goal

Make the IFF screen answer the first local identity question:

```text
Кто заявлен как свой в локальном списке?
```

This phase must not pretend that local roster membership proves physical
proximity, current position, or direction.

## Scope

- Add a fixed local roster:
  - `Вы`
  - `Петя`
  - `Вася`
  - `Женя`
- Show the roster on the `КОМАНДА` tab.
- Selecting a roster entry opens the `КОНТАКТ` tab for that participant.
- Keep the `Я ПОДХОЖУ` state attached only to local player `Вы`.
- Show confidence fields separately:
  - `identity`
  - `proximity`
  - `position`
  - `direction`

## Files

- `app/src/main/java/net/afterday/compas/IffActivity.java`
- `app/src/main/res/layout/iff_activity.xml`
- `app/src/main/res/values/ids.xml`
- `.planning/ROADMAP.md`
- `.planning/STATE.md`

## Verification

- Build debug APK with `:app:assembleDebug`.
- If an ADB device is connected:
  - install the debug APK;
  - launch Compass;
  - tap `IFF`;
  - open `КОМАНДА`;
  - select a participant;
  - confirm `КОНТАКТ` updates;
  - tap `Я ПОДХОЖУ`;
  - confirm the local player shows approach state while radio/proximity remain
    unconfirmed.

## Out Of Scope

- No network exchange.
- No cryptography or team token.
- No GPS calibration.
- No Wi-Fi calibration.
- No Samsung-specific logic.
- No visual redesign beyond what the roster requires.
