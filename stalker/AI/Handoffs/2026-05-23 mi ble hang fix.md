# 2026-05-23 Mi BLE hang fix

## Context

During the next field walk, Mi (`83efb856`, `zhenya`) constantly hung and the operator only reached OnePlus.

## Evidence

Crash buffer was empty. The app stayed on `IffActivity`, but Mi became unresponsive.

Logcat showed repeated Bluetooth GATT registrations from `net.afterday.compas` and UI frame skips:

- `BtGatt.GattService registerClient() ... name=net.afterday.compas`
- `Skipped 483 frames`
- `Skipped 1449 frames`

`dumpsys bluetooth_manager` showed multiple GATT client entries for the app PID.

## Root Cause

Mi's old Bluetooth stack is fragile around BLE advertising. After GPS and advertising updates, the app could trigger repeated BLE advertiser registration attempts. When the stack returned advertiser errors/status delays, the app retried and caused UI stalls.

## Fixes

Build `1844-ble-advertise-pending-guard` is installed on Mi.

Changes:

- BLE advertising payload is now stable identity-only for MVP (`GPS_IN_BLE_ADVERTISE_ENABLED = false`).
- GPS is still collected/logged locally; it is no longer embedded into BLE advertising, so GPS jumps do not restart the BLE beacon.
- BLE advertising content restarts are throttled by policy.
- If BLE advertiser startup is pending, `startAdvertise()` refuses overlapping starts.
- After repeated advertiser failures, advertising is disabled for the current app session instead of retrying forever; BLE scan and app UI continue.
- Added source guard tests:
  - `scripts/test-iff-ble-field-radio-stability-guard.ps1`
  - `scripts/test-iff-ble-advertise-failure-guard.ps1`

## Verification

Passed:

- `scripts/test-iff-ble-advertise-failure-guard.ps1`
- `scripts/test-iff-ble-field-radio-stability-guard.ps1`
- `scripts/test-iff-ble-payload.ps1`
- `gradle :app:assembleDebug`

Mi verification:

- Installed `versionCode=1844`, `versionName=1844-ble-advertise-pending-guard`.
- Launched `net.afterday.compas/.IffActivity`.
- Waited 90 seconds, then another 60 seconds.
- No crash / ANR.
- `dumpsys activity top` returned normally.
- `mPendingInputEventCount=0`.
- No new `registerClient()` loop after startup.

Known limitation:

- Mi still has a startup lag around BLE initialization (`Skipped 244` and `126` frames). It did not continue after startup. For field test, wait several seconds after opening the app before walking.

## Next Field Step

Use Mi as `zhenya` target again. Keep Samsung (`petya`) and OnePlus (`vasya`) as stationary anchors. After opening the app on Mi, wait 10 seconds; if the UI remains responsive, walk the planned route.

## 2026-05-23 11:40 update: post-walk analysis

Mi was returned after the walk and then the remaining phones were connected. Logs were captured under:

- `artifacts/field-run-20260523-mi-after-walk/`
- `artifacts/field-run-20260523-mi-after-walk/summary.md`
- `artifacts/field-run-20260523-mi-after-walk/locator-comparison/`

Fresh 11:33-11:36 window:

- Mi stayed alive: no crash, `IffActivity` active, `mPendingInputEventCount=0`.
- OnePlus saw Mi (`zhenya`) 379 times with RSSI `-106..-27`, average about `-55 dBm`.
- Mi saw OnePlus (`vasya`) and Samsung (`petya`), but Samsung did not produce fresh `ble_target_observation` for Mi in that interval.
- Result: OnePlus had `ONE_ANCHOR` / `FIELD_RADIO_RSSI`; stable `TWO_ANCHORS` did not form.
- Mi GPS later degraded into rejected jumps around `1.5-1.6 km`, so GPS remained diagnostic only.

After capture, build `1844-ble-advertise-pending-guard` was installed on OnePlus and Samsung too. All three phones are now on `1844`.

Next practical test should verify both anchors see Mi in the same 15-second window. Place OnePlus and Samsung with less wall shielding and walk Mi through a line where it is visible to both anchors.
