# 2026-05-23 Mi Wi-Fi Direct crash fix

## Context

Samsung (`petya`) and OnePlus (`vasya`) were already placed as anchors. Mi (`zhenya`) was connected and appeared to hang before the next walk test.

## Finding

Mi did not hang inside the app. The process crashed and Android returned to launcher.

Crash:

```text
java.lang.IllegalArgumentException: service request is null
    at android.net.wifi.p2p.WifiP2pManager.addServiceRequest(...)
    at net.afterday.compas.iff.IffWifiDirectDiscoveryTransport.addServiceRequestAndDiscover(...)
```

## Fix

Build `1840-wfd-null-request-guard` prevents direct `addServiceRequest(..., serviceRequest)` calls.

Changes:

- `IffWifiDirectDiscoveryTransport` now creates Wi-Fi Direct DNS-SD requests through `newServiceRequest()`.
- If typed DNS-SD request creation fails or returns null, it falls back to `WifiP2pDnsSdServiceRequest.newInstance()`.
- If no request can be created, the app logs `wifi_direct_service_request ok=false reason=null_request` and skips Wi-Fi Direct service discovery instead of crashing.
- Synchronous `addServiceRequest` exceptions are caught and logged.
- Added `scripts/test-iff-wifi-direct-service-request-guard.ps1`.

## Verification

Passed:

- `scripts/test-iff-wifi-direct-service-request-guard.ps1`
- `scripts/test-iff-wifi-direct-payload.ps1`
- `scripts/test-iff-field-locator.ps1`
- `gradle :app:assembleDebug`

Installed on Mi (`83efb856`):

- `versionCode=1840`
- `versionName=1840-wfd-null-request-guard`

After launch and waiting past one Wi-Fi Direct refresh interval:

- current focus remained `net.afterday.compas/.IffActivity`
- crash buffer was empty

## Next Field Step

Keep Samsung and OnePlus as stationary anchors. Walk with Mi (`zhenya`) as target. After returning, connect phones and analyze whether `TWO_ANCHORS`, clock direction, and distance bucket follow the route.
