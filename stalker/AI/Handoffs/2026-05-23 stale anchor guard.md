# 2026-05-23 stale anchor guard

## What changed

Build: `1846-stale-anchor-guard`

The 13:52 Mi walk showed that raw `WIFI_TARGET` could use one fresh anchor and one old anchor RSSI sample. Example pattern:

- `left=vasya:-49 ageMs=26000`
- `right=petya:-46 ageMs=1230`
- raw locator still produced `clock=12`

This made the clock jump even when the opposite anchor was stale.

## Fix

`IffWifiTargetObservationStore` now has two freshness windows:

- `LOCATOR_FRESH_MS = 8000`: only observations newer than this can form two-anchor geometry.
- `DISPLAY_FRESH_MS = 30000`: older observations may still be shown as context.

Compact status now marks old-but-visible anchors as:

```text
left=vasya:old rssi=-49 ageMs=12000
```

Old anchors no longer match the map readiness regex for `TWO_ANCHORS`, so the operator snapshot can hold the last stable fix instead of rendering a noisy raw direction.

## Verified

- `scripts/test-iff-field-locator.ps1`
- `scripts/test-iff-wifi-target-locator.ps1`
- `scripts/test-field-locator-comparison.ps1`
- `gradle :app:assembleDebug`

Installed and launched on:

- Mi `83efb856`
- OnePlus `e089985a`
- Samsung `R3CT20C8A8N`

Crash buffers were empty after launch. Logs were cleared before the next run.

## Next run expectation

During the next Mi walk:

- Fresh two-anchor data should still produce `WIFI_TARGET_STABLE`.
- If one anchor relay goes older than 8 seconds, raw locator should drop to one-anchor/no two-anchor, and the map should show `WIFI_TARGET_HOLD` from the operator snapshot instead of a jumping clock.

## 14:13 Mi walk result

Artifact: `artifacts/field-run-20260523-1413-mi-walk/summary.md`

Window: `2026-05-23 14:13:37..14:18:40 +03`

Observed:

- Crash buffers empty on all three phones.
- `auto_field_check` had no gaps over 4 seconds, despite screens turning off.
- Screens did turn off:
  - Mi slept `14:15:37..14:16:09`, plus a brief power-button sleep `14:17:13..14:17:14`.
  - OnePlus screen off `14:13:50`, wake `14:17:35`.
  - Samsung off/on cycles around `14:14:39..14:15:14` and `14:17:27..14:17:43`.
- BLE observation volume was good: Mi 364, OnePlus 727, Samsung 1246 valid `zhenya` samples in the test window.
- Wi-Fi Direct service exchange worked, but in bursts. Fresh two-anchor `WIFI_TARGET` counts were sparse: Mi 4, OnePlus 18, Samsung 8.
- `old rssi=` markers appeared frequently, confirming the stale-anchor guard is active and no longer lets old opposite-anchor RSSI form fresh geometry.

Next:

- Add diagnostic logging for operator snapshot output (`WIFI_TARGET_STABLE` / `WIFI_TARGET_HOLD`), because current logs expose raw `WIFI_TARGET` but not the held UI/operator decision.
- Consider adding `FLAG_KEEP_SCREEN_ON` for the field/game screens. The service survived screen-off, but operator-visible field mode should not sleep during a run.

## 1848 operator keep-awake/logging

Build: `1848-operator-field-keepawake-logfix`

Implemented:

- `IffActivity` now uses `FLAG_KEEP_SCREEN_ON`.
- `IffForegroundRadioService` now keeps its own `IffOperatorFieldSnapshotStore` and writes `operatorFieldMapStatus` into every `auto_field_check`.
- `operatorFieldMapStatus` is flat key/value text, for example:
  - `source=FIELD_RADIO_RSSI readiness=ONE_ANCHOR distance=10m clock=na visible=true directionKnown=false statusLine=FIELD_RADIO_RSSI 10m clock=na ONE_ANCHOR`
- `compare-field-locator-options.ps1` now preserves `OperatorFieldMapStatus` in `locator-events.csv`.
- Added source-level regression test `scripts/test-iff-field-operator-diagnostics.ps1`.

Verified:

- `scripts/test-iff-field-operator-diagnostics.ps1`
- `scripts/test-iff-field-locator.ps1`
- `scripts/test-iff-wifi-target-locator.ps1`
- `scripts/test-field-locator-comparison.ps1`
- `scripts/test-iff-diagnostics-start.ps1`
- `scripts/test-iff-ui-no-mock-controls.ps1`
- `gradle :app:assembleDebug`

Installed and launched on:

- Mi `83efb856`
- OnePlus `e089985a`
- Samsung `R3CT20C8A8N`

ADB field profile:

- `screen_off_timeout=1800000`
- `stay_on_while_plugged_in`: Mi `7`, OnePlus/Samsung report `15`
- app added to device-idle whitelist where supported
- runtime/appops permissions attempted; Mi Android 6 rejects modern Android 12+ permissions and `cmd appops`, expected for that SDK

Fresh diagnostics confirmed `operatorFieldMapStatus` on all three devices. Crash buffers had no `net.afterday.compas` crash after launch.

## 15:48 Mi walk result

Artifact: `artifacts/field-run-20260523-1548-mi-walk/summary.md`

Window: `2026-05-23 15:48:59..15:53:36 +03`

Observed:

- No crashes. No `auto_field_check` gaps over 4 seconds.
- BLE `zhenya` observations in window: Mi 363, OnePlus 823, Samsung 366.
- Wi-Fi Direct service receive worked on anchors:
  - OnePlus received `petya` 57 times and `zhenya` 11 times.
  - Samsung received `zhenya` 15 times and `vasya` 24 times.
- `operatorFieldMapStatus` produced real two-anchor operator output:
  - OnePlus `WIFI_TARGET_STABLE` mostly `25m clock=2`, plus `25m clock=1` and `25m clock=12`.
  - OnePlus `WIFI_TARGET_HOLD` continued `25m clock=1/12` after relay gaps.
  - Samsung `WIFI_TARGET_STABLE` had `25m clock=3`; Samsung `WIFI_TARGET_HOLD` had `25m clock=2/12`.
- Mi mostly had `NO_ANCHORS` during the walk, which is acceptable for the carried target; anchors are the useful operator screens.

Interpretation:

- The operator snapshot layer is now proven in the field: raw `WIFI_TARGET` bursts are converted into `WIFI_TARGET_STABLE` and short `WIFI_TARGET_HOLD`.
- Next useful test is a longer straight-line outdoor pass with static anchors and a known stop side, to check whether the modal clock sector converges for 20-40 seconds.

## 1849 playable direction hold

Build: `1849-playable-direction-hold`

Reason:

- User observed on Samsung that the direction arrow appeared only about 2 times per 30 seconds.
- Logs confirmed the problem: raw/stable two-anchor Wi-Fi appears in short bursts, while previous hold window was only 8 seconds.

Change:

- `IffOperatorFieldSnapshotStore.HOLD_MS` increased from `8000` to `30000`.
- This keeps the last valid two-anchor direction visible as `WIFI_TARGET_HOLD` for up to 30 seconds.
- The status line already carries `ageMs=...`, so diagnostics distinguish fresh `WIFI_TARGET_STABLE` from older held direction.

Tests:

- Added operator store regression coverage:
  - hold survives sparse field updates at 28 seconds;
  - hold expires after the 30 second playable window.
- Verified:
  - `scripts/test-iff-field-locator.ps1`
  - `scripts/test-iff-field-operator-diagnostics.ps1`
  - `scripts/test-iff-wifi-target-locator.ps1`
  - `scripts/test-field-locator-comparison.ps1`
  - `gradle :app:assembleDebug`

Installed and launched on Mi, OnePlus, Samsung.

## 1851 shadow-guarded Wi-Fi target

Build: `1851-shadow-guarded-wifi-target`

Reason:

- Outdoor run around `2026-05-23 16:19` showed rare direction and frequent wrong `3 o'clock`.
- Samsung log example at `16:19:33`: `vasya=-92dBm ageMs=786`, `petya=-45dBm ageMs=400`, `deltaDb=47`, raw locator `20m clock=3`.
- User expected roughly `12 o'clock`; the observed `3 o'clock / 20m` was caused by treating a one-sided shadow/noise-floor anchor as a valid two-anchor geometry.

Change:

- `IffWifiTargetLocator` now rejects two-anchor Wi-Fi target direction when one anchor is at or below `-88dBm` and the anchor delta is at least `20dB`.
- Rejected shadowed Wi-Fi target returns `INSUFFICIENT_DATA`, so the field locator falls back to radio RSSI distance without direction instead of showing a confident-looking wrong arrow.

Tradeoff:

- Fewer arrows in one-sided/shadowed geometry.
- The arrows that remain should be less likely to jump to false `3/9 o'clock` because of a barely heard opposite anchor.

Verified:

- Added regression test for `vasya=-92dBm`, `petya=-45dBm` falling back to `FIELD_RADIO_RSSI`.
- `scripts/test-iff-field-locator.ps1`
- `scripts/test-iff-wifi-target-locator.ps1`
- `scripts/test-iff-field-operator-diagnostics.ps1`
- `scripts/test-field-locator-comparison.ps1`

## 1852 GPS ground truth and tighter shadow guard

Build: `1852-gps-ground-truth-shadow-guard`

Reason:

- Outdoor swapped-anchor run `2026-05-23 17:01..17:05`:
  - Samsung showed direction rarely.
  - Correct `11 o'clock` appeared only once by user observation.
  - False `3 o'clock` still appeared.
- Analysis artifact: `artifacts/field-run-20260523-1701-swap-mi-one/locator-comparison`.
- In the run window Samsung had:
  - `FIELD_RADIO_RSSI`: 99 ticks.
  - `WIFI_TARGET`: 18 ticks.
  - `INSUFFICIENT_DATA`: 2 ticks.
  - `GPS_ASSISTED`: 2 ticks.
- False right-side readings still came from weak/shadowed Wi-Fi target fixes such as:
  - `left=vasya:-93`, `right=petya:-74`, `deltaDb=19`, raw `clock=3`.
  - Previous 1851 guard rejected `>=20dB`; this was too loose.

Change:

- Tightened Wi-Fi target shadow guard from `20dB` to `16dB` when one anchor is at or below `-88dBm`.
- Added explicit GPS ground-truth fields to `auto_field_check` diagnostics:
  - `gpsLocalLatE7`, `gpsLocalLonE7`, `gpsLocalAgeMs`, `gpsLocalAccuracyM`.
  - `gpsRemoteLatE7`, `gpsRemoteLonE7`, `gpsRemoteAgeMs`, `gpsRemoteAccuracyM`.
  - `gpsRawDistanceM`, `gpsRawBearingDeg`.
- Added `latE7/lonE7/cluster` to `gps_location_update` and rejected-location diagnostics.
- Updated `compare-field-locator-options.ps1` so `locator-events.csv` preserves these GPS fields.

Intent:

- Next field log can be checked against actual local/remote GPS geometry instead of only rounded clusters.
- Marginal one-sided Wi-Fi geometry should stop producing false `3 o'clock`; better no arrow than a wrong arrow.

Verified:

- Added regression test for `left=-93dBm`, `right=-74dBm` falling back to `FIELD_RADIO_RSSI`.
- Added analyzer regression for GPS ground-truth CSV fields.
- `scripts/test-iff-field-locator.ps1`
- `scripts/test-iff-wifi-target-locator.ps1`
- `scripts/test-iff-field-operator-diagnostics.ps1`
- `scripts/test-field-locator-comparison.ps1`

## 1850 short hold faster valid

Build: `1850-short-hold-faster-valid`

Reason:

- User correctly noted that 30 seconds of held direction can be 60m+ of target movement, so it is too stale for field guidance.

Change:

- Reduced operator direction hold from `30000ms` to `10000ms`.
- Increased two-anchor Wi-Fi locator freshness from `8000ms` to `10000ms`.
- Practical intent: accept the next valid two-anchor result a little more often, but do not display a held arrow for longer than about 10 seconds.

Tradeoff:

- Compared with `1848`, the arrow should appear a bit more often because a 9-10s delayed anchor can still participate.
- Compared with `1849`, stale held direction is much less dangerous.
- Worst-case direction age is still diagnostic-visible via `ageMs=...`; field decisions should treat `WIFI_TARGET_HOLD` differently from `WIFI_TARGET_STABLE`.

Verified:

- `scripts/test-iff-field-locator.ps1`
- `scripts/test-iff-wifi-target-locator.ps1`
- `scripts/test-iff-field-operator-diagnostics.ps1`
- `scripts/test-field-locator-comparison.ps1`
- `gradle :app:assembleDebug`

Installed and launched on Mi, OnePlus, Samsung.
