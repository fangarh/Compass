# Phase 16: IFF Cautious Proximity UI

**Status:** completed

**Goal:** Make IFF proximity wording safer after the second field run so that a
fresh medium RSSI is shown as a weak hint, not as a strong or exact distance
confirmation.

**Scope:**

- Keep the same RSSI threshold bands.
- Rename the medium fresh radio band from `RADIO_MID` to `RADIO_WEAK_HINT`.
- Rename the weak edge band from `RADIO_WEAK` to `RADIO_EDGE_HINT`.
- Lower medium/edge confidence scores so only `RADIO_NEAR` counts as strong
  proximity in the team summary.
- Change the team summary from `PROXIMITY OK` to `PROXIMITY STRONG`.
- Keep identity, position, and direction independent.

**Out of scope:**

- Network exchange.
- Cryptography.
- GPS or direction inference.
- Samsung-specific behavior.
- RSSI threshold tuning.

**Verification:**

- `:app:assembleDebug` completed successfully.
- APK installed on OnePlus `e089985a`.
- UIAutomator verified main PDA -> `IFF`.
- Team screen showed:
  - `RADIO FRESH: 0`;
  - `PROXIMITY STRONG: 0`;
  - `DIRECTION: UNKNOWN`.

**Resulting proximity labels:**

| Input state | Label | Score | Meaning |
| --- | --- | ---: | --- |
| fresh RSSI >= -55 dBm | `RADIO_NEAR` | 75% | Strong proximity hint, no azimuth. |
| fresh RSSI -70..-56 dBm | `RADIO_WEAK_HINT` | 45% | Fresh signal heard, distance not exact. |
| fresh RSSI < -70 dBm | `RADIO_EDGE_HINT` | 30% | Weak fresh signal, only audibility. |
| stale radio | `STALE_RADIO` | 25% | Old memory, not current proof. |
| missing or older than stale window | `UNKNOWN` | 0% | No usable radio proof. |
