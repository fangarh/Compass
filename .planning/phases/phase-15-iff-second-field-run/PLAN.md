# Phase 15: IFF Second Field Run

**Status:** completed

**Goal:** Collect a second controlled IFF field run with repeated samples for
near, far, shielded, cabinet-shielded, and off states.

**Scope:**

- Use Samsung `SM-S908B` as hotspot `COMPASS_IFF_PETYA`.
- Use OnePlus `NE2215` / `e089985a` as receiver and IFF recording screen.
- Record multiple `ЗАПИСАТЬ` snapshots per state.
- Pull the diagnostic log and run the existing analyzer with named windows.
- Keep runtime thresholds unchanged until evidence is reviewed.

**Out of scope:**

- New Android code.
- Network exchange.
- Cryptographic identity.
- GPS/direction calibration.
- Samsung-specific logic.

**Verification command:**

```powershell
powershell -ExecutionPolicy Bypass -File scripts\analyze-field-logs.ps1 `
  -InputRoot artifacts\iff-field-session-20260519-1643 `
  -OutputDir artifacts\iff-field-analysis-20260519-1643 `
  -Windows "near=2026-05-19 16:35:25..2026-05-19 16:35:50;far=2026-05-19 16:37:25..2026-05-19 16:38:05;return_near=2026-05-19 16:38:15..2026-05-19 16:38:40;body_shielded=2026-05-19 16:39:05..2026-05-19 16:39:40;cabinet_shielded=2026-05-19 16:40:35..2026-05-19 16:41:10;off=2026-05-19 16:42:00..2026-05-19 16:43:05" `
  -BeaconSsids "COMPASS_IFF_*"
```

Result: analyzer completed over 1 log file with 27644 scan entries and wrote
the IFF field-check timeline, summary CSV, and Markdown report.

**Field summary:**

| State | Samples | Proximity | Avg RSSI | RSSI range | Avg age | Interpretation |
| --- | ---: | --- | ---: | --- | ---: | --- |
| near | 3 | `RADIO_NEAR 75%` | -28.0 dBm | -29..-27 | 4386 ms | Strong near signal. |
| far, 5-7 m + walls | 3 | `RADIO_MID 55%` | -59.7 dBm | -63..-56 | 6900 ms | Useful weaker proximity hint. |
| return near | 2 | `RADIO_NEAR 75%` | -21.0 dBm | -30..-12 | 2240 ms | Strong recovery after return. |
| body-shielded | 3 | `RADIO_NEAR 75%` | -45.3 dBm | -49..-40 | 3207 ms | Still near despite body shielding. |
| cabinet-shielded | 3 | `RADIO_NEAR 75%` | -43.7 dBm | -45..-43 | 6395 ms | Still near despite cabinet/body obstruction. |
| off | 3 | `STALE_RADIO` then `UNKNOWN` | -40.0 dBm | -40..-40 | 77337 ms | Freshness gating works; old RSSI does not prove proximity. |

**Threshold notes:**

- The current `RADIO_NEAR` cutoff is conservative enough for close/shielded
  office cases in this run: near/shielded stayed stronger than about -50 dBm.
- The far office sample through walls clustered around -60 dBm and stayed
  `RADIO_MID`, which matches the MVP intent: weaker proximity hint, not a
  claim of exact distance.
- Freshness is more important than RSSI after hotspot shutdown: stale/unknown
  correctly dropped identity to `ROSTER_ONLY` and proximity to `25%`/`0%`.
- Do not derive direction from the strong return-near or shielded RSSI spikes.
