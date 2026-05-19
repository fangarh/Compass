# Phase 18: IFF Remote Witness Contract

**Status:** completed

**Goal:** Define the local data contract for future remote witness reports
without adding network transport or cryptographic verification yet.

**Scope:**

- Add `IffRemoteWitnessReport` with contract version
  `iff-remote-witness-v1`.
- Add `IffRemoteWitnessStore` as an in-memory receiver for future reports.
- Include source player, target player, target beacon SSID, BSSID, RSSI,
  frequency, observed/received monotonic times, and signature status.
- Keep signature as `SIGNATURE_PENDING`.
- Feed remote report lists into `IffWitnessQuorum`.
- Show `REMOTE REPORTS` and contract/signature placeholder on the team/contact
  UI.
- Record remote contract fields in `IFF_DIAG event=field_check`.
- Extend analyzer CSV/Markdown output with remote contract fields.

**Out of scope:**

- Real network transport.
- Real cryptographic signatures.
- GPS or direction inference.
- Raising identity confidence from remote reports.
- Samsung-specific behavior.

**Verification:**

- `:app:assembleDebug` completed successfully.
- APK installed on OnePlus `e089985a`.
- UIAutomator verified main PDA -> `IFF`.
- Team screen showed:
  - `REMOTE REPORTS: 0`;
  - `Remote witness contract: iff-remote-witness-v1`;
  - `Signature status пока placeholder: SIGNATURE_PENDING`.
- `ЗАПИСАТЬ` for `Петя` produced:
  `remoteWitnessContract=iff-remote-witness-v1 remoteReportCount=0 remoteFreshSources=0`.
- `scripts/test-analyze-field-logs.ps1` passed.
- Fresh contract log analyzer check completed:
  `artifacts/iff-field-session-20260519-1707` ->
  `artifacts/iff-field-analysis-20260519-1707`, 635 scan entries, CSV rows
  with `iff-remote-witness-v1` and `0 reports / 0 fresh`.

**Decision:**

Remote reports may raise proximity quorum later, but identity confidence remains
separate until signature verification exists.
