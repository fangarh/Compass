# IFF Team Roster Management Design

## Goal

Add a local mechanism for adding and removing IFF team members from the phone UI.

## Selected Behavior

- The `TEAM` screen shows only confirmed team members.
- Radio-discovered phones that are not in the team appear only after the operator explicitly enables `SEARCH`.
- Removing a member from `TEAM` is permanent for normal UI: the member disappears from the team list, contact flow, and map.
- A removed member can be added again only through the `SEARCH` procedure.
- Removed members are not auto-restored just because BLE/Wi-Fi keeps hearing them.
- The local device identity cannot be removed while it is the current `THIS DEVICE`.

## Architecture

- Add a pure Java `IffTeamRosterStore` that owns roster serialization, default seed members, removed-member tracking, and candidate filtering.
- Keep the existing `IffActivity` UI style: simple full-width buttons, no new screens or fragments.
- Use current radio data as discovery input:
  - `IffForegroundRadioService` exposes participant states learned from coordinate exchange.
  - `IffRadioWitnessStore` exposes current direct radio witnesses.
- Filter `IffParticipantMapModel.Snapshot` to team player IDs so removed members do not appear on the radar/map.

## Persistence

Use the existing `iff` shared preferences:

- `team_roster` - serialized confirmed team members.
- `team_removed_players` - serialized removed player IDs.

Existing prefs for local identity, display names, trust, radio, and map scale stay unchanged.

## UI

- `TEAM` gets a `SEARCH` toggle button under the local name button.
- When search is on, discovered non-team candidates are listed with `ADD`.
- Contact cards for non-local team members get `REMOVE FROM TEAM`.
- If there are no candidates, search shows a short empty-state line.

## Testing

Add a JVM script test for `IffTeamRosterStore`:

- default seed contains the current office test members;
- remove hides a member from team and marks it removed;
- discovered removed member can be added again explicitly;
- serialization round-trips team and removed lists;
- invalid/empty IDs are rejected.
