# Coordinate IFF Core Design

## Context

Current IFF behavior was built around temporary field-test concepts: fixed left/right anchors, a single target, RSSI-derived clock direction, and a tactical map that colors "known direction" as green. Field tests on May 24 showed that this model is brittle: phones can correctly exchange proximity evidence while the map still points at the wrong participant or refuses to show direction.

The new model replaces the anchor/target map with a participant coordinate map. The current Android application, permissions, foreground service, and existing transport entry points remain as the host shell for the first implementation.

## Goals

- Show only participants whose coordinates are known and usable.
- Treat all participants symmetrically: each phone can publish itself and relay known neighbors.
- Use GPS coordinates as the only source of map geometry.
- Keep RSSI as supporting metadata only, never as a source of direction.
- Make green mean "this participant pressed approach", not "direction is known".
- Keep the first field build small enough to install and test quickly on the three phones.

## Non-Goals

- Do not rewrite the whole Android app.
- Do not build an internet relay in the first slice.
- Do not continue the fixed `target / left anchor / right anchor` model.
- Do not infer map direction from BLE or Wi-Fi RSSI.
- Do not delete old IFF classes until the new core is field-tested.

## Architecture

The first slice adds a new coordinate-based IFF core beside the old IFF code.

- `IffParticipantState`: immutable state for one participant.
- `IffParticipantStore`: in-memory store of the best known state per participant.
- `IffCoordinateMessage`: transport-neutral payload that carries participant states.
- `IffCoordinateExchange`: converts local GPS and known neighbors into outgoing messages, and merges incoming messages.
- `IffParticipantMapModel`: produces UI-ready map points relative to the local participant.
- `IffParticipantMapView`: new map drawing path with no static anchors.

Old field-map classes can remain during transition, but the IFF screen should render the new participant map once the first slice is connected.

## Participant State

Each participant state contains:

- `playerId`: participant identity, for example `petya`, `vasya`, `zhenya`.
- `latE7`, `lonE7`: GPS coordinates.
- `accuracyM`: GPS accuracy.
- `observedElapsedMs`: local monotonic time when this state was received or created.
- `gpsFixTimeMs`: source GPS wall-clock time if available.
- `sourcePlayerId`: who supplied this state.
- `hopCount`: `0` for self, `1` for direct neighbor, higher for relayed.
- `approachActive`: true when that participant pressed "я подхожу".

State is usable for map geometry only when coordinates are valid, age is inside the configured freshness window, and accuracy is not worse than the configured map threshold.

## Coordinate Exchange

Outgoing messages include:

- the local participant's current state, when GPS is usable;
- known neighbor states that are fresh enough to relay;
- approach state for the sender and any relayed participant state.

Merge rules:

- Prefer lower `hopCount`.
- Prefer fresher GPS fix for the same participant and hop count.
- Ignore states with invalid coordinates.
- Do not accept relayed states with excessive age or hop count.
- Do not replace a fresh local self-state with a relayed self-state.

The payload format should be transport-neutral so BLE, Wi-Fi Direct, UDP, and a future relay can all feed the same core.

## Map Behavior

The map is centered on the local participant when local GPS is usable.

For each remote participant with usable coordinates:

- calculate distance and bearing from local coordinates;
- place the point relative to the local center;
- show distance, coordinate age, source, and hop count;
- draw green when `approachActive=true`;
- draw neutral/amber/gray based on freshness when `approachActive=false`.

If local GPS is not usable, the map shows a list-style degraded state: known participants can be listed with age/source, but spatial positions are hidden because relative geometry would be misleading.

No fixed anchors are drawn. No participant is special on the map because of role names.

## Approach State

The existing "я подхожу" action becomes a participant state flag.

- Pressing the button sets local `approachActive=true` for a short TTL.
- The flag is included in coordinate messages.
- Remote maps color that participant green while the flag is fresh.
- Green does not imply GPS quality or direction quality.

## Transport Plan

First slice:

- Use existing foreground service and GPS update loop.
- Feed local GPS updates into the new participant store.
- Encode/decode coordinate messages through the existing Wi-Fi Direct/UDP path first.
- Keep BLE as optional direct-neighbor input only if it can carry the new payload without destabilizing advertising.

Later:

- Add internet relay as another transport that consumes and produces `IffCoordinateMessage`.

## Diagnostics

Each auto snapshot should log:

- local player id;
- map mode: `SPATIAL`, `NO_LOCAL_GPS`, or `NO_PARTICIPANTS`;
- visible participant count;
- hidden participant count with reasons;
- per participant: id, age, accuracy, distance, bearing, source, hop count, approach flag;
- transport receive/transmit counts by transport.

The logs must make it clear why a participant is not shown.

## Testing

Unit tests cover:

- participant state merge precedence;
- stale/weak coordinate rejection;
- relayed coordinate hop handling;
- approach flag TTL and propagation;
- map point generation from local and remote coordinates;
- degraded mode when local GPS is missing;
- serialization round trip for coordinate messages.

Field validation criteria:

- Update cadence remains at least once every 2 seconds.
- With three phones together, all three see each other within one field window.
- The participant who pressed "я подхожу" appears green on other phones.
- Moving one phone out of Wi-Fi visibility does not create fake anchors or fake directions.
- Direction is shown only when local and remote coordinate pairs are fresh enough.

## Migration

Implementation should be additive first:

1. Build and test the new core without changing the current field service behavior.
2. Feed new diagnostics alongside old diagnostics.
3. Switch the IFF map view to the new participant model.
4. Field-test on the three phones.
5. Remove or disable old anchor/target map code after the new model is confirmed.

