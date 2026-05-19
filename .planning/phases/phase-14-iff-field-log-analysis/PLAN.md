# Phase 14: IFF Field Log Analysis

**Status:** completed

**Goal:** Turn the captured near/far/off IFF diagnostic session into a compact
field report and threshold notes for the MVP confidence model.

**Scope:**

- Extend `scripts/analyze-field-logs.ps1` to parse
  `IFF_DIAG event=field_check`.
- Export IFF field-check timeline and summary CSV files.
- Add an IFF field-check section to the analyzer Markdown report.
- Keep runtime Android behavior unchanged.

**Out of scope:**

- Network exchange.
- Cryptography.
- GPS calibration.
- Wi-Fi calibration for a specific Samsung/OnePlus model.
- Direction/azimuth inference from RSSI.

**Verification:**

```powershell
powershell -ExecutionPolicy Bypass -File scripts\analyze-field-logs.ps1 `
  -InputRoot artifacts\iff-field-session-20260519-1613 `
  -OutputDir artifacts\iff-field-analysis-20260519-1613 `
  -Windows "near=2026-05-19 16:09:45..2026-05-19 16:10:10;far=2026-05-19 16:10:20..2026-05-19 16:10:55;off=2026-05-19 16:12:20..2026-05-19 16:13:25" `
  -BeaconSsids "COMPASS_IFF_*"
```

Result: analyzer completed over 1 log file with 8036 scan entries and wrote
`iff-field-checks.csv`, `iff-field-check-summary.csv`, and `summary.md`.

**Field report:**

| State | Identity | Proximity | RSSI | Age | Interpretation |
| --- | --- | --- | ---: | ---: | --- |
| near | `ROSTER_PLUS_RADIO_CLAIM 60%` | `RADIO_NEAR 75%` | -39 dBm | 2269 ms | Strong fresh proximity hint. |
| far | `ROSTER_PLUS_RADIO_CLAIM 60%` | `RADIO_MID 55%` | -68 dBm | 12161 ms | Weak but still fresh proximity hint. |
| off/stale | `ROSTER_ONLY 40%` | `STALE_RADIO 25%` | -57 dBm | 28759 ms | Recent radio memory, not current proximity proof. |
| off/unknown | `ROSTER_ONLY 40%` | `UNKNOWN 0%` | -57 dBm | 70886 ms | No usable current radio proof. |

**Threshold notes:**

- Current thresholds are directionally useful for a coarse MVP: `RADIO_NEAR`
  at roughly -39 dBm, `RADIO_MID` at roughly -68 dBm, stale after roughly 20 s,
  unknown around 60 s.
- RSSI remains only a proximity hint. It must not raise `direction` or
  `position` confidence.
- The `far` sample had only one field-check point and mixed beacon scan
  strength in the wider window, so avoid tightening thresholds from this one
  session alone.
- For the next field run, collect repeated `near`, `far`, `body-shielded`,
  `pocket`, and `off` records before changing runtime thresholds.
