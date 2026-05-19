# Requirements

## R1: Preserve Recovered App Baseline

The application must continue to build and launch as `net.afterday.compas`.
Existing gameplay, inventory, scanner, foreground service, and radio influence
behavior must not be changed by diagnostic-only work.

## R2: Wi-Fi Field Diagnostics

The app must support field diagnostics for Wi-Fi scanning on a real Android
phone. Diagnostics must distinguish scan requests, scan result broadcasts,
cached result publication, fresh result age, and scan result contents.

## R3: File-Based Log Export

Field diagnostics must be written to an app-owned file on the phone. After a
test run, the file must be retrievable over ADB without relying on Logcat.

## R4: Physical Device Validation

Radio behavior must be validated on the physical Samsung SM-S908B device
`R3CT20C8A8N`. The emulator may be used for build or launch smoke checks, but
it is not sufficient for Wi-Fi radio conclusions.

## R5: Obsidian Sync

New decisions, test procedures, and field findings must be reflected in
`stalker/` documents when they affect project direction or operational practice.
