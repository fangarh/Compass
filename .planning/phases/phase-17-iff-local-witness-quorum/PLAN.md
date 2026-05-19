# Phase 17: IFF Local Witness Quorum

**Status:** completed

**Goal:** Add the first multi-witness foundation without pretending that remote
phones already exchange reports.

**Scope:**

- Add a local witness quorum model for a selected target player.
- Treat the current phone as `local-device`.
- Show remote teammate reports as `PENDING` until network exchange exists.
- Add quorum state to `КОНТАКТ`, `КОМАНДА`, and `КАРТА`.
- Record quorum fields in `IFF_DIAG event=field_check`.
- Extend the field-log analyzer to export quorum fields.

**Out of scope:**

- Network transport.
- Cryptography.
- Direction inference.
- GPS geometry.
- Raising identity confidence from quorum.

**Verification:**

- `:app:assembleDebug` completed successfully.
- APK installed on OnePlus `e089985a`.
- UIAutomator verified main PDA -> `IFF`.
- Team screen showed `MULTI-WITNESS: 0`.
- Selecting `Петя` showed `WITNESSES: NO_CURRENT_WITNESS 0/3`.
- Contact details showed `WITNESS QUORUM` with:
  - `local-device: NO_REPORT`;
  - `remote teammate reports: PENDING (network not implemented)`;
  - identity not upgraded without crypto.
- `ЗАПИСАТЬ` produced:
  `witnessQuorum=NO_CURRENT_WITNESS witnessFreshSources=0 witnessPossibleSources=3`.
- `scripts/test-analyze-field-logs.ps1` passed.
- Analyzer verified the fresh quorum log:
  `artifacts/iff-field-session-20260519-1659` ->
  `artifacts/iff-field-analysis-20260519-1659`, 2398 scan entries, quorum CSV
  row `NO_CURRENT_WITNESS 0/3`.

**Decision:**

Quorum can raise proximity confidence later when multiple fresh sources exist,
but it must not raise identity confidence without cryptographic identity.
