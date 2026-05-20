# Phase 23: IFF Phone-to-Phone UDP RX Verification

**Status:** completed

## Goal

Verify that the Phase 22 UDP transport stub can carry an unsigned remote
witness report between two physical phones on the same Wi-Fi network.

## Scope

- Use the existing `TX STUB` transport.
- Keep the report contract at `iff-remote-witness-v1`.
- Keep signature state at `SIGNATURE_PENDING`.
- Verify receive on the second phone through IFF UI and diagnostics.
- Do not upgrade identity or proximity from unsigned transport alone.

## Out Of Scope

- Code changes.
- Crypto.
- Trust decisions.
- GPS or Wi-Fi calibration.
- Direction inference.
- Production discovery or pairing.

## Test Setup

- Samsung `R3CT20C8A8N`: `10.14.135.249/24` on `swlan0`.
- OnePlus `e089985a`: `10.14.135.40/24` on `wlan0`.
- Shared broadcast address: `10.14.135.255`.
- Both phones opened `Main -> IFF`.
- Both screens showed `TRANSPORT: udp:45873 ... listening`.

## Verification

- Samsung `TX STUB` emitted a packet but OnePlus did not show RX in that
  direction during this pass.
- OnePlus `TX STUB` emitted a packet and Samsung received it.
- Samsung diagnostic log recorded:
  - `event=remote_witness_received`
  - `sourcePlayerId=debug-ne2215`
  - `targetPlayerId=local-you`
  - `freshness=REMOTE_FRESH`
  - `signatureStatus=SIGNATURE_PENDING`
- Samsung diagnostic log recorded:
  - `event=remote_witness_udp_rx`
  - `accepted=true`
  - `from=10.14.135.40`
  - `contract=iff-remote-witness-v1`
- Samsung `IFF_DIAG event=field_check` recorded:
  - `remoteReportCount=1`
  - `remoteFreshSources=0`
  - `remoteStaleSources=1`
  - `transportStatus="udp:45873 tx=1 rx=1 rejected=0 rx local-you"`
- Analyzer run completed on `artifacts/iff-field-session-20260520-1005` and
  wrote `artifacts/iff-field-analysis-20260520-1005`.

## Result

Phone-to-phone UDP RX is proven in at least the OnePlus-to-Samsung direction on
the shared Wi-Fi network. The UI correctly keeps the witness as remote evidence
only and does not turn unsigned transport into identity/proximity proof.
