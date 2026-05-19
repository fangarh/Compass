# Phase 9 Plan: IFF MVP Skeleton

## MVP Direction

The real MVP is the Obsidian IFF scenario:

```text
Кто-то рядом -> это заявленный свой? -> насколько уверенно? -> где примерно?
```

The app should confirm known teammates. It must not claim enemy detection and
must not treat arbitrary phone names, SSIDs, or Bluetooth names as identity.

## Full MVP Road

1. IFF screen skeleton.
2. Local roster and trusted player identity model.
3. Manual `Я ПОДХОЖУ` state with timeout.
4. Phone-to-phone approach signal.
5. Witness model: who hears whom, signal freshness, and relative strength.
6. Confidence model split into identity, proximity, position, and direction.
7. Team screen ordering by tactical priority.
8. Map screen with GPS error circles and witness links.
9. Field MVP test with at least two teammates and one approaching player.

## Current Slice

Add the UI container and local approach state only.

## Scope

- Add an explicit `IFF` button on the existing PDA main screen.
- Add a separate full-screen `IffActivity`.
- Add tabs:
  - `КОНТАКТ`
  - `КОМАНДА`
  - `КАРТА`
- Add a local `Я ПОДХОЖУ` button that switches to a visible local approach
  state and auto-expires after two minutes.
- Display honest placeholder states: no roster, no witnesses, no radio
  confirmation.

## Files

- `app/src/main/java/net/afterday/compas/MainActivity.java`
- `app/src/main/java/net/afterday/compas/IffActivity.java`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/layout/iff_activity.xml`
- `app/src/main/res/layout-port/activity_main.xml`
- `app/src/main/res/layout-land/activity_main.xml`
- `app/src/main/res/values/ids.xml`
- `.planning/ROADMAP.md`
- `.planning/STATE.md`
- `stalker/Решения/2026-05-19 MVP IFF Roadmap.md`

## Verification

- Build `:app:assembleDebug`.
- Install APK on a physical phone.
- Launch main PDA.
- Tap `IFF`.
- Confirm IFF tabs render.
- Tap `Я ПОДХОЖУ`.
- Confirm the screen shows `ВЫ ПОДХОДИТЕ` and still states that identity,
  proximity, witnesses, and radio confirmation are not implemented.

## Out Of Scope

- No phone-to-phone protocol yet.
- No roster persistence yet.
- No cryptographic/team token yet.
- No map implementation beyond placeholder state.
- No changes to existing gameplay influence calculations.
