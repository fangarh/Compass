# Phase 26: IFF Tactical Map Mock

**Status:** completed

## Goal

Add a truthful tactical map placeholder to the IFF screen so the MVP can start
showing roster and radio-witness state spatially without claiming GPS position
or bearing.

## Scope

- Add a custom `IffTacticalMapView` for the `КАРТА` tab.
- Render the local device, roster slots, selected contact, and radio freshness
  colors.
- Label the map as a mock and explicitly state `NO GPS POSITION / NO BEARING`.
- Keep the map status at `POSITION/DIRECTION: UNKNOWN 0%`.
- Keep slot placement as roster order only, not direction.
- Preserve BLE field radio evidence as freshness/proximity witness only.

## Out Of Scope

- Real map SDK integration.
- GPS placement or calibration.
- Direction, azimuth, or triangulation.
- Cryptographic identity proof.
- Background BLE lifecycle.
- Samsung-specific or OnePlus-specific map/radio logic.

## Verification

- `:app:assembleDebug` completed successfully.
- APK installed on Samsung `R3CT20C8A8N` and OnePlus `e089985a`.
- UIAutomator verified `Main -> IFF -> КАРТА` on both phones.
- Both phones showed `КАРТА`, `mock карта: freshness без азимута`, and
  `POSITION/DIRECTION: UNKNOWN 0%`.
- Both phones exposed the custom map canvas view on the `КАРТА` tab.
- OnePlus screenshot confirmed the canvas renders the grid, mock labels,
  roster points, and BLE freshness color without presenting GPS or bearing as
  known.

## Result

The IFF screen now has a map-shaped tactical surface for future witness
visualization. It is deliberately not a real navigation map yet: it shows radio
freshness and roster state while keeping exact position and direction unknown.
