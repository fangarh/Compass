# Coordinate IFF Core Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a new coordinate-based IFF core and map that shows participants from exchanged GPS coordinates, with green meaning "approach declared".

**Architecture:** Add the new coordinate core beside the legacy IFF code, then switch the IFF map rendering path to the new participant map. Existing foreground service, GPS permission flow, and transport shell remain the host for the first field-testable slice.

**Tech Stack:** Android Java, existing `net.afterday.compas.iff` package, PowerShell test scripts, `javac`-based unit tests, Gradle debug APK.

---

## File Structure

- Create `app/src/main/java/net/afterday/compas/iff/IffParticipantState.java`: immutable participant coordinate state.
- Create `app/src/main/java/net/afterday/compas/iff/IffParticipantStore.java`: best-known participant state merge/store.
- Create `app/src/main/java/net/afterday/compas/iff/IffCoordinateMessage.java`: transport-neutral coordinate message encode/decode.
- Create `app/src/main/java/net/afterday/compas/iff/IffParticipantMapModel.java`: converts participant store + local player into map points.
- Create `scripts/test-data/iff-coordinate-core/IffCoordinateCoreTest.java`: unit tests for state/store/message/map model.
- Create `scripts/test-iff-coordinate-core.ps1`: compiles and runs new coordinate core tests.
- Modify `app/src/main/java/net/afterday/compas/iff/IffForegroundRadioService.java`: feed local GPS and diagnostics into the new participant store.
- Modify `app/src/main/java/net/afterday/compas/iff/IffWifiDirectPayload.java`: add coordinate message fields without removing old fields.
- Modify `app/src/main/java/net/afterday/compas/iff/IffWifiDirectDiscoveryTransport.java`: transmit/receive coordinate messages.
- Modify `app/src/main/java/net/afterday/compas/iff/IffTacticalMapView.java`: render participant map points, no static anchors.
- Modify `app/src/main/java/net/afterday/compas/IffActivity.java`: pass new participant map model to the view and bind "я подхожу" to approach state.
- Modify existing test scripts only to include new tests where needed; do not delete legacy tests during the transition.

## Task 1: Participant State And Store

**Files:**
- Create: `app/src/main/java/net/afterday/compas/iff/IffParticipantState.java`
- Create: `app/src/main/java/net/afterday/compas/iff/IffParticipantStore.java`
- Create: `scripts/test-data/iff-coordinate-core/IffCoordinateCoreTest.java`
- Create: `scripts/test-iff-coordinate-core.ps1`

- [ ] **Step 1: Write the failing store tests**

Create `scripts/test-data/iff-coordinate-core/IffCoordinateCoreTest.java` with these initial tests:

```java
import java.util.List;
import net.afterday.compas.iff.IffParticipantState;
import net.afterday.compas.iff.IffParticipantStore;

public final class IffCoordinateCoreTest {
    public static void main(String[] args) {
        keepsFreshLowerHopParticipantState();
        rejectsRelayedSelfStateOverLocalState();
    }

    private static void keepsFreshLowerHopParticipantState() {
        IffParticipantStore store = new IffParticipantStore();
        store.merge(IffParticipantState.create(
                "petya", 599914900, 303262808, 8, 1000L, 100000L,
                "petya", 0, false));
        store.merge(IffParticipantState.create(
                "petya", 599914901, 303262809, 8, 2000L, 101000L,
                "vasya", 1, false));

        IffParticipantState state = store.get("petya");

        assertNotNull(state, "petya state should exist");
        assertEquals(0, state.hopCount);
        assertEquals("petya", state.sourcePlayerId);
    }

    private static void rejectsRelayedSelfStateOverLocalState() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        store.merge(IffParticipantState.create(
                "vasya", 601171000, 313860000, 5, 1000L, 100000L,
                "vasya", 0, false));
        store.merge(IffParticipantState.create(
                "vasya", 599914900, 303262808, 10, 3000L, 101000L,
                "petya", 1, false));

        IffParticipantState state = store.get("vasya");

        assertNotNull(state, "self state should exist");
        assertEquals(601171000L, state.latE7);
        assertEquals(0, state.hopCount);
    }

    private static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
```

- [ ] **Step 2: Add the new test runner**

Create `scripts/test-iff-coordinate-core.ps1`:

```powershell
$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$javaHome = $env:JAVA_HOME
if ([string]::IsNullOrWhiteSpace($javaHome)) {
    throw "JAVA_HOME is not set"
}

$javac = Join-Path $javaHome "bin\javac.exe"
$java = Join-Path $javaHome "bin\java.exe"
$outDir = Join-Path $root "artifacts\test-iff-coordinate-core\classes"
New-Item -ItemType Directory -Force $outDir | Out-Null

$sources = @(
    (Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffParticipantState.java"),
    (Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffParticipantStore.java"),
    (Join-Path $root "scripts\test-data\iff-coordinate-core\IffCoordinateCoreTest.java")
)

& $javac -encoding UTF-8 -d $outDir $sources
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& $java -cp $outDir IffCoordinateCoreTest
if ($LASTEXITCODE -ne 0) {
    throw "IffCoordinateCoreTest failed with exit code $LASTEXITCODE"
}

Write-Host "IFF coordinate core test passed."
```

- [ ] **Step 3: Run test to verify RED**

Run: `scripts\test-iff-coordinate-core.ps1`

Expected: FAIL because `IffParticipantState.java` and `IffParticipantStore.java` do not exist.

- [ ] **Step 4: Implement `IffParticipantState`**

Create `app/src/main/java/net/afterday/compas/iff/IffParticipantState.java`:

```java
package net.afterday.compas.iff;

public final class IffParticipantState {
    public final String playerId;
    public final long latE7;
    public final long lonE7;
    public final int accuracyM;
    public final long observedElapsedMs;
    public final long gpsFixTimeMs;
    public final String sourcePlayerId;
    public final int hopCount;
    public final boolean approachActive;

    private IffParticipantState(
            String playerId,
            long latE7,
            long lonE7,
            int accuracyM,
            long observedElapsedMs,
            long gpsFixTimeMs,
            String sourcePlayerId,
            int hopCount,
            boolean approachActive) {
        this.playerId = safe(playerId);
        this.latE7 = latE7;
        this.lonE7 = lonE7;
        this.accuracyM = accuracyM;
        this.observedElapsedMs = observedElapsedMs;
        this.gpsFixTimeMs = gpsFixTimeMs;
        this.sourcePlayerId = safe(sourcePlayerId);
        this.hopCount = Math.max(0, hopCount);
        this.approachActive = approachActive;
    }

    public static IffParticipantState create(
            String playerId,
            long latE7,
            long lonE7,
            int accuracyM,
            long observedElapsedMs,
            long gpsFixTimeMs,
            String sourcePlayerId,
            int hopCount,
            boolean approachActive) {
        if (playerId == null || playerId.length() == 0) {
            return null;
        }
        if (!validCoordinateE7(latE7, lonE7)) {
            return null;
        }
        return new IffParticipantState(
                playerId,
                latE7,
                lonE7,
                Math.max(0, accuracyM),
                Math.max(0L, observedElapsedMs),
                Math.max(0L, gpsFixTimeMs),
                sourcePlayerId == null || sourcePlayerId.length() == 0 ? playerId : sourcePlayerId,
                hopCount,
                approachActive);
    }

    public long ageMs(long nowElapsedMs) {
        return Math.max(0L, nowElapsedMs - observedElapsedMs);
    }

    public IffParticipantState relayedBy(String relayPlayerId, long nowElapsedMs) {
        return create(
                playerId,
                latE7,
                lonE7,
                accuracyM,
                nowElapsedMs,
                gpsFixTimeMs,
                relayPlayerId,
                hopCount + 1,
                approachActive);
    }

    private static boolean validCoordinateE7(long latE7, long lonE7) {
        return latE7 >= -900000000L && latE7 <= 900000000L
                && lonE7 >= -1800000000L && lonE7 <= 1800000000L;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
```

- [ ] **Step 5: Implement `IffParticipantStore`**

Create `app/src/main/java/net/afterday/compas/iff/IffParticipantStore.java`:

```java
package net.afterday.compas.iff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class IffParticipantStore {
    private final Object lock = new Object();
    private final Map<String, IffParticipantState> states = new HashMap<>();
    private final String localPlayerId;

    public IffParticipantStore() {
        this("");
    }

    public IffParticipantStore(String localPlayerId) {
        this.localPlayerId = safe(localPlayerId);
    }

    public boolean merge(IffParticipantState next) {
        if (next == null) {
            return false;
        }
        synchronized (lock) {
            IffParticipantState previous = states.get(next.playerId);
            if (!shouldReplace(previous, next)) {
                return false;
            }
            states.put(next.playerId, next);
            return true;
        }
    }

    public IffParticipantState get(String playerId) {
        synchronized (lock) {
            return states.get(safe(playerId));
        }
    }

    public List<IffParticipantState> all() {
        synchronized (lock) {
            return new ArrayList<>(states.values());
        }
    }

    public void clear() {
        synchronized (lock) {
            states.clear();
        }
    }

    private boolean shouldReplace(IffParticipantState previous, IffParticipantState next) {
        if (previous == null) {
            return true;
        }
        if (next.playerId.equals(localPlayerId) && next.hopCount > 0 && previous.hopCount == 0) {
            return false;
        }
        if (next.hopCount < previous.hopCount) {
            return true;
        }
        if (next.hopCount > previous.hopCount) {
            return false;
        }
        if (next.gpsFixTimeMs != previous.gpsFixTimeMs) {
            return next.gpsFixTimeMs > previous.gpsFixTimeMs;
        }
        return next.observedElapsedMs >= previous.observedElapsedMs;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
```

- [ ] **Step 6: Run test to verify GREEN**

Run: `scripts\test-iff-coordinate-core.ps1`

Expected: PASS with `IFF coordinate core test passed.`

## Task 2: Coordinate Message Serialization

**Files:**
- Create: `app/src/main/java/net/afterday/compas/iff/IffCoordinateMessage.java`
- Modify: `scripts/test-data/iff-coordinate-core/IffCoordinateCoreTest.java`
- Modify: `scripts/test-iff-coordinate-core.ps1`

- [ ] **Step 1: Add failing round-trip tests**

Extend `IffCoordinateCoreTest.main`:

```java
roundTripsCoordinateMessage();
rejectsMalformedCoordinateMessage();
```

Add methods:

```java
private static void roundTripsCoordinateMessage() {
    java.util.List<IffParticipantState> states = new java.util.ArrayList<>();
    states.add(IffParticipantState.create(
            "petya", 599914900, 303262808, 8, 1000L, 100000L,
            "petya", 0, true));
    states.add(IffParticipantState.create(
            "vasya", 601171000, 313860000, 12, 2000L, 100500L,
            "zhenya", 1, false));

    String encoded = net.afterday.compas.iff.IffCoordinateMessage.encode("zhenya", 42L, states);
    net.afterday.compas.iff.IffCoordinateMessage.Parsed parsed =
            net.afterday.compas.iff.IffCoordinateMessage.parse(encoded, 3000L);

    assertNotNull(parsed, "coordinate message should parse");
    assertEquals("zhenya", parsed.senderPlayerId);
    assertEquals(42L, parsed.sequence);
    assertEquals(2, parsed.states.size());
    assertEquals("petya", parsed.states.get(0).playerId);
    assertEquals(1, parsed.states.get(1).hopCount);
}

private static void rejectsMalformedCoordinateMessage() {
    net.afterday.compas.iff.IffCoordinateMessage.Parsed parsed =
            net.afterday.compas.iff.IffCoordinateMessage.parse("bad|payload", 1000L);
    if (parsed != null) {
        throw new AssertionError("malformed coordinate message should be rejected");
    }
}
```

- [ ] **Step 2: Update the test runner source list**

Add `IffCoordinateMessage.java` to `scripts/test-iff-coordinate-core.ps1`:

```powershell
(Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffCoordinateMessage.java"),
```

- [ ] **Step 3: Run test to verify RED**

Run: `scripts\test-iff-coordinate-core.ps1`

Expected: FAIL because `IffCoordinateMessage` does not exist.

- [ ] **Step 4: Implement coordinate message**

Create `app/src/main/java/net/afterday/compas/iff/IffCoordinateMessage.java`:

```java
package net.afterday.compas.iff;

import java.util.ArrayList;
import java.util.List;

public final class IffCoordinateMessage {
    public static final String PREFIX = "CIFF2";

    private IffCoordinateMessage() {
    }

    public static String encode(String senderPlayerId, long sequence, List<IffParticipantState> states) {
        StringBuilder builder = new StringBuilder();
        builder.append(PREFIX)
                .append("|sender=").append(clean(senderPlayerId))
                .append("|seq=").append(Math.max(0L, sequence));
        if (states != null) {
            for (int i = 0; i < states.size(); i++) {
                IffParticipantState state = states.get(i);
                if (state == null) {
                    continue;
                }
                builder.append("|p=")
                        .append(clean(state.playerId)).append(",")
                        .append(state.latE7).append(",")
                        .append(state.lonE7).append(",")
                        .append(state.accuracyM).append(",")
                        .append(state.gpsFixTimeMs).append(",")
                        .append(clean(state.sourcePlayerId)).append(",")
                        .append(state.hopCount).append(",")
                        .append(state.approachActive ? "1" : "0");
            }
        }
        return builder.toString();
    }

    public static Parsed parse(String encoded, long nowElapsedMs) {
        if (encoded == null || !encoded.startsWith(PREFIX + "|")) {
            return null;
        }
        String sender = "";
        long sequence = 0L;
        List<IffParticipantState> states = new ArrayList<>();
        String[] parts = encoded.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.startsWith("sender=")) {
                sender = part.substring("sender=".length());
            } else if (part.startsWith("seq=")) {
                sequence = parseLong(part.substring("seq=".length()), 0L);
            } else if (part.startsWith("p=")) {
                IffParticipantState state = parseState(part.substring(2), nowElapsedMs);
                if (state != null) {
                    states.add(state);
                }
            }
        }
        if (sender.length() == 0) {
            return null;
        }
        return new Parsed(sender, sequence, states);
    }

    private static IffParticipantState parseState(String value, long nowElapsedMs) {
        String[] fields = value.split(",");
        if (fields.length != 8) {
            return null;
        }
        return IffParticipantState.create(
                fields[0],
                parseLong(fields[1], Long.MIN_VALUE),
                parseLong(fields[2], Long.MIN_VALUE),
                (int) parseLong(fields[3], -1L),
                nowElapsedMs,
                parseLong(fields[4], 0L),
                fields[5],
                (int) parseLong(fields[6], 0L),
                "1".equals(fields[7]));
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace("|", "").replace(",", "");
    }

    public static final class Parsed {
        public final String senderPlayerId;
        public final long sequence;
        public final List<IffParticipantState> states;

        Parsed(String senderPlayerId, long sequence, List<IffParticipantState> states) {
            this.senderPlayerId = senderPlayerId;
            this.sequence = sequence;
            this.states = states;
        }
    }
}
```

- [ ] **Step 5: Run test to verify GREEN**

Run: `scripts\test-iff-coordinate-core.ps1`

Expected: PASS.

## Task 3: Participant Map Model

**Files:**
- Create: `app/src/main/java/net/afterday/compas/iff/IffParticipantMapModel.java`
- Modify: `scripts/test-data/iff-coordinate-core/IffCoordinateCoreTest.java`
- Modify: `scripts/test-iff-coordinate-core.ps1`

- [ ] **Step 1: Add failing map model tests**

Add tests:

```java
buildsSpatialMapFromLocalAndRemoteCoordinates();
degradesWhenLocalGpsMissing();
```

Add methods:

```java
private static void buildsSpatialMapFromLocalAndRemoteCoordinates() {
    IffParticipantStore store = new IffParticipantStore("vasya");
    store.merge(IffParticipantState.create(
            "vasya", 599914900, 303262808, 5, 1000L, 100000L,
            "vasya", 0, false));
    store.merge(IffParticipantState.create(
            "petya", 599915800, 303262808, 8, 1200L, 100200L,
            "petya", 0, true));

    net.afterday.compas.iff.IffParticipantMapModel.Snapshot snapshot =
            net.afterday.compas.iff.IffParticipantMapModel.from(store, "vasya", 2000L);

    assertEquals("SPATIAL", snapshot.mode);
    assertEquals(1, snapshot.points.size());
    assertEquals("petya", snapshot.points.get(0).playerId);
    assertTrue(snapshot.points.get(0).approachActive, "approach flag should render green");
    assertTrue(snapshot.points.get(0).distanceM > 0, "distance should be computed");
}

private static void degradesWhenLocalGpsMissing() {
    IffParticipantStore store = new IffParticipantStore("vasya");
    store.merge(IffParticipantState.create(
            "petya", 599915800, 303262808, 8, 1200L, 100200L,
            "petya", 0, true));

    net.afterday.compas.iff.IffParticipantMapModel.Snapshot snapshot =
            net.afterday.compas.iff.IffParticipantMapModel.from(store, "vasya", 2000L);

    assertEquals("NO_LOCAL_GPS", snapshot.mode);
    assertEquals(0, snapshot.points.size());
    assertEquals(1, snapshot.hiddenCount);
}
```

Add helper:

```java
private static void assertTrue(boolean value, String message) {
    if (!value) {
        throw new AssertionError(message);
    }
}
```

- [ ] **Step 2: Update runner and verify RED**

Add `IffParticipantMapModel.java` to `scripts/test-iff-coordinate-core.ps1`, then run:

`scripts\test-iff-coordinate-core.ps1`

Expected: FAIL because map model does not exist.

- [ ] **Step 3: Implement map model**

Create `app/src/main/java/net/afterday/compas/iff/IffParticipantMapModel.java`:

```java
package net.afterday.compas.iff;

import java.util.ArrayList;
import java.util.List;

public final class IffParticipantMapModel {
    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final long MAP_FRESH_MS = 30000L;
    private static final int MAP_MAX_ACCURACY_M = 100;

    private IffParticipantMapModel() {
    }

    public static Snapshot from(IffParticipantStore store, String localPlayerId, long nowElapsedMs) {
        if (store == null) {
            return new Snapshot("NO_LOCAL_GPS", new ArrayList<Point>(), 0, "store missing");
        }
        IffParticipantState local = store.get(localPlayerId);
        if (!usable(local, nowElapsedMs)) {
            int hidden = 0;
            List<IffParticipantState> states = store.all();
            for (int i = 0; i < states.size(); i++) {
                if (!safe(states.get(i).playerId).equals(safe(localPlayerId))) {
                    hidden++;
                }
            }
            return new Snapshot("NO_LOCAL_GPS", new ArrayList<Point>(), hidden, "local coordinates unavailable");
        }
        List<Point> points = new ArrayList<>();
        int hidden = 0;
        List<IffParticipantState> states = store.all();
        for (int i = 0; i < states.size(); i++) {
            IffParticipantState state = states.get(i);
            if (state == null || state.playerId.equals(localPlayerId)) {
                continue;
            }
            if (!usable(state, nowElapsedMs)) {
                hidden++;
                continue;
            }
            points.add(pointFrom(local, state, nowElapsedMs));
        }
        return new Snapshot(points.isEmpty() ? "NO_PARTICIPANTS" : "SPATIAL",
                points, hidden, points.isEmpty() ? "no usable remote participants" : "ok");
    }

    private static boolean usable(IffParticipantState state, long nowElapsedMs) {
        return state != null
                && state.ageMs(nowElapsedMs) <= MAP_FRESH_MS
                && state.accuracyM <= MAP_MAX_ACCURACY_M;
    }

    private static Point pointFrom(IffParticipantState local, IffParticipantState remote, long nowElapsedMs) {
        double localLat = local.latE7 / 10000000.0d;
        double localLon = local.lonE7 / 10000000.0d;
        double remoteLat = remote.latE7 / 10000000.0d;
        double remoteLon = remote.lonE7 / 10000000.0d;
        int distanceM = (int) Math.round(distanceMeters(localLat, localLon, remoteLat, remoteLon));
        int bearingDeg = (int) Math.round(bearingDegrees(localLat, localLon, remoteLat, remoteLon));
        float radius = Math.min(0.46f, Math.max(0.12f, distanceM / 50.0f * 0.46f));
        double angleRad = Math.toRadians(bearingDeg);
        float x = clamp(0.5f + (float) Math.sin(angleRad) * radius, 0.12f, 0.88f);
        float y = clamp(0.5f - (float) Math.cos(angleRad) * radius, 0.16f, 0.82f);
        return new Point(
                remote.playerId,
                x,
                y,
                distanceM,
                bearingDeg,
                remote.ageMs(nowElapsedMs),
                remote.accuracyM,
                remote.sourcePlayerId,
                remote.hopCount,
                remote.approachActive);
    }

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        return EARTH_RADIUS_M * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }

    private static double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);
        double dLon = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dLon) * Math.cos(rLat2);
        double x = Math.cos(rLat1) * Math.sin(rLat2)
                - Math.sin(rLat1) * Math.cos(rLat2) * Math.cos(dLon);
        double degrees = Math.toDegrees(Math.atan2(y, x));
        return degrees < 0.0d ? degrees + 360.0d : degrees;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Snapshot {
        public final String mode;
        public final List<Point> points;
        public final int hiddenCount;
        public final String reason;

        Snapshot(String mode, List<Point> points, int hiddenCount, String reason) {
            this.mode = mode;
            this.points = points;
            this.hiddenCount = hiddenCount;
            this.reason = reason;
        }
    }

    public static final class Point {
        public final String playerId;
        public final float x;
        public final float y;
        public final int distanceM;
        public final int bearingDeg;
        public final long ageMs;
        public final int accuracyM;
        public final String sourcePlayerId;
        public final int hopCount;
        public final boolean approachActive;

        Point(String playerId, float x, float y, int distanceM, int bearingDeg,
              long ageMs, int accuracyM, String sourcePlayerId, int hopCount, boolean approachActive) {
            this.playerId = playerId;
            this.x = x;
            this.y = y;
            this.distanceM = distanceM;
            this.bearingDeg = bearingDeg;
            this.ageMs = ageMs;
            this.accuracyM = accuracyM;
            this.sourcePlayerId = sourcePlayerId;
            this.hopCount = hopCount;
            this.approachActive = approachActive;
        }
    }
}
```

- [ ] **Step 4: Run test to verify GREEN**

Run: `scripts\test-iff-coordinate-core.ps1`

Expected: PASS.

## Task 4: Approach State

**Files:**
- Create: `app/src/main/java/net/afterday/compas/iff/IffApproachState.java`
- Modify: `scripts/test-data/iff-coordinate-core/IffCoordinateCoreTest.java`
- Modify: `scripts/test-iff-coordinate-core.ps1`
- Modify later integration in `IffActivity.java`

- [ ] **Step 1: Add failing approach TTL test**

Add:

```java
approachExpiresAfterTtl();
```

Test method:

```java
private static void approachExpiresAfterTtl() {
    net.afterday.compas.iff.IffApproachState state =
            new net.afterday.compas.iff.IffApproachState(30000L);
    state.activate(1000L);

    assertTrue(state.isActive(2000L), "approach should be active inside ttl");
    assertTrue(!state.isActive(32000L), "approach should expire after ttl");
}
```

- [ ] **Step 2: Update runner and verify RED**

Add `IffApproachState.java` to `scripts/test-iff-coordinate-core.ps1`.

Run: `scripts\test-iff-coordinate-core.ps1`

Expected: FAIL because `IffApproachState` does not exist.

- [ ] **Step 3: Implement approach state**

Create `app/src/main/java/net/afterday/compas/iff/IffApproachState.java`:

```java
package net.afterday.compas.iff;

public final class IffApproachState {
    private final long ttlMs;
    private long activeUntilElapsedMs;

    public IffApproachState(long ttlMs) {
        this.ttlMs = Math.max(1000L, ttlMs);
    }

    public synchronized void activate(long nowElapsedMs) {
        activeUntilElapsedMs = Math.max(0L, nowElapsedMs) + ttlMs;
    }

    public synchronized void clear() {
        activeUntilElapsedMs = 0L;
    }

    public synchronized boolean isActive(long nowElapsedMs) {
        return activeUntilElapsedMs > 0L && nowElapsedMs <= activeUntilElapsedMs;
    }
}
```

- [ ] **Step 4: Run test to verify GREEN**

Run: `scripts\test-iff-coordinate-core.ps1`

Expected: PASS.

## Task 5: Feed Local GPS Into New Core

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/iff/IffForegroundRadioService.java`
- Modify: `app/src/main/java/net/afterday/compas/IffActivity.java`

- [ ] **Step 1: Add static new-core accessors to the foreground service**

In `IffForegroundRadioService`, add fields:

```java
private static final IffParticipantStore PARTICIPANTS = new IffParticipantStore();
private static final IffApproachState APPROACH = new IffApproachState(30000L);
```

Add methods:

```java
public static IffParticipantMapModel.Snapshot participantMapSnapshot(String localPlayerId) {
    return IffParticipantMapModel.from(PARTICIPANTS, localPlayerId, SystemClock.elapsedRealtime());
}

public static void activateApproach() {
    APPROACH.activate(SystemClock.elapsedRealtime());
}
```

- [ ] **Step 2: Feed accepted local GPS into `PARTICIPANTS`**

In `rememberLocation(Location location, String source)`, after `latestLocation = new Location(location);`, merge self state:

```java
PARTICIPANTS.merge(IffParticipantState.create(
        localPlayerId,
        IffRemoteWitnessFrame.coordinateE7(location.getLatitude()),
        IffRemoteWitnessFrame.coordinateE7(location.getLongitude()),
        location.hasAccuracy() ? Math.round(location.getAccuracy()) : 100,
        SystemClock.elapsedRealtime(),
        location.getTime(),
        localPlayerId,
        0,
        APPROACH.isActive(SystemClock.elapsedRealtime())));
```

- [ ] **Step 3: Add diagnostics to auto snapshot**

In `recordAutoFieldCheckSnapshot`, compute:

```java
IffParticipantMapModel.Snapshot participantMap =
        IffParticipantMapModel.from(PARTICIPANTS, currentLocalPlayerId, SystemClock.elapsedRealtime());
```

Append to the existing diagnostic event:

```java
+ " participantMapStatus=\"" + clean(participantMapStatus(participantMap)) + "\""
```

Add helper:

```java
private static String participantMapStatus(IffParticipantMapModel.Snapshot snapshot) {
    if (snapshot == null) {
        return "mode=NO_LOCAL_GPS visible=0 hidden=0 reason=missing";
    }
    StringBuilder builder = new StringBuilder();
    builder.append("mode=").append(snapshot.mode)
            .append(" visible=").append(snapshot.points.size())
            .append(" hidden=").append(snapshot.hiddenCount)
            .append(" reason=").append(clean(snapshot.reason));
    for (int i = 0; i < snapshot.points.size(); i++) {
        IffParticipantMapModel.Point point = snapshot.points.get(i);
        builder.append(" p").append(i).append("=")
                .append(point.playerId)
                .append(":").append(point.distanceM).append("m")
                .append("/").append(point.bearingDeg).append("deg")
                .append("/age=").append(point.ageMs)
                .append("/acc=").append(point.accuracyM)
                .append("/src=").append(clean(point.sourcePlayerId))
                .append("/hop=").append(point.hopCount)
                .append("/approach=").append(point.approachActive);
    }
    return builder.toString();
}
```

- [ ] **Step 4: Build to verify integration**

Run: `gradle :app:assembleDebug`

Expected: BUILD SUCCESSFUL.

## Task 6: Coordinate Messages Through Wi-Fi Direct

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/iff/IffWifiDirectPayload.java`
- Modify: `app/src/main/java/net/afterday/compas/iff/IffWifiDirectDiscoveryTransport.java`
- Modify: `scripts/test-data/iff-wifi-direct-payload/IffWifiDirectPayloadTest.java`
- Modify: `scripts/test-iff-wifi-direct-payload.ps1` if new source files are required.

- [ ] **Step 1: Add failing payload test**

In `IffWifiDirectPayloadTest`, add a test that attaches a coordinate message string:

```java
private static void carriesCoordinateMessage() {
    Map<String, String> txt = IffWifiDirectPayload.build("vasya", 7L, 123456L);
    txt.put(IffWifiDirectPayload.KEY_COORDINATES,
            "CIFF2|sender=vasya|seq=7|p=vasya,599914900,303262808,8,100000,vasya,0,1");

    IffWifiDirectPayload.Parsed parsed = IffWifiDirectPayload.parse(txt);

    assertNotNull(parsed, "payload should parse");
    assertEquals("CIFF2|sender=vasya|seq=7|p=vasya,599914900,303262808,8,100000,vasya,0,1",
            parsed.coordinateMessage);
}
```

Call it from `main`.

- [ ] **Step 2: Run RED**

Run: `scripts\test-iff-wifi-direct-payload.ps1`

Expected: FAIL because `KEY_COORDINATES` and `coordinateMessage` do not exist.

- [ ] **Step 3: Extend payload class**

In `IffWifiDirectPayload`, add:

```java
public static final String KEY_COORDINATES = "coords";
```

In `Parsed`, add:

```java
public final String coordinateMessage;
```

Set it from TXT map:

```java
this.coordinateMessage = safe(coordinateMessage);
```

When building outgoing TXT records in transport, put:

```java
txt.put(IffWifiDirectPayload.KEY_COORDINATES, coordinateMessage);
```

Do not remove legacy target observation fields in this task.

- [ ] **Step 4: Merge received coordinate message**

In `IffWifiDirectDiscoveryTransport`, where parsed payloads are handled:

```java
IffCoordinateMessage.Parsed coordinates =
        IffCoordinateMessage.parse(parsed.coordinateMessage, SystemClock.elapsedRealtime());
if (coordinates != null) {
    for (int i = 0; i < coordinates.states.size(); i++) {
        IffForegroundRadioService.mergeParticipantState(coordinates.states.get(i));
    }
}
```

Add `public static void mergeParticipantState(IffParticipantState state)` to `IffForegroundRadioService`:

```java
public static void mergeParticipantState(IffParticipantState state) {
    PARTICIPANTS.merge(state);
}
```

- [ ] **Step 5: Run tests**

Run:

```powershell
scripts\test-iff-coordinate-core.ps1
scripts\test-iff-wifi-direct-payload.ps1
gradle :app:assembleDebug
```

Expected: all pass.

## Task 7: Switch Tactical Map Rendering To Participants

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/iff/IffTacticalMapView.java`
- Modify: `app/src/main/java/net/afterday/compas/IffActivity.java`

- [ ] **Step 1: Add participant map state to `IffTacticalMapView`**

Add field:

```java
private IffParticipantMapModel.Snapshot participantState;
```

Add setter:

```java
public void setParticipantState(IffParticipantMapModel.Snapshot nextParticipantState) {
    this.participantState = nextParticipantState;
    invalidate();
}
```

- [ ] **Step 2: Stop drawing static anchors**

In `drawFieldGeometry`, remove the fixed anchor circles and labels:

```java
canvas.drawCircle(leftX, cy, dp(10), paint);
canvas.drawCircle(rightX, cy, dp(10), paint);
canvas.drawText("VASYA", ...);
canvas.drawText("PETYA", ...);
```

Keep the range rings and local center marker.

- [ ] **Step 3: Draw participant points**

Add method:

```java
private void drawParticipantPoints(Canvas canvas, int width, int height) {
    if (participantState == null || participantState.points.size() == 0) {
        return;
    }
    for (int i = 0; i < participantState.points.size(); i++) {
        IffParticipantMapModel.Point point = participantState.points.get(i);
        float x = width * point.x;
        float y = height * point.y;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(point.approachActive ? 0xff7dff73 : 0xffffd16a);
        canvas.drawCircle(x, y, dp(11), paint);
        textPaint.setColor(0xffffffff);
        canvas.drawText(point.playerId, x + dp(16), y - dp(4), textPaint);
        textPaint.setColor(0xffb8c49a);
        canvas.drawText(point.distanceM + "m " + point.bearingDeg + "deg h" + point.hopCount,
                x + dp(16), y + dp(13), textPaint);
    }
}
```

Call it from `onDraw` after `drawFieldGeometry`.

- [ ] **Step 4: Update legend text**

Change legend to:

```java
canvas.drawText("green=approach declared  amber=coordinate contact", dp(12), y + dp(17), textPaint);
```

- [ ] **Step 5: Feed participant snapshot from `IffActivity.render()`**

Where the tactical map currently receives `setFieldState(...)`, also call:

```java
tacticalMap.setParticipantState(
        IffForegroundRadioService.participantMapSnapshot(localDevicePlayerId));
```

- [ ] **Step 6: Build**

Run: `gradle :app:assembleDebug`

Expected: BUILD SUCCESSFUL.

## Task 8: Bind "Я Подхожу" To Green Participant State

**Files:**
- Modify: `app/src/main/java/net/afterday/compas/IffActivity.java`
- Modify: `app/src/main/java/net/afterday/compas/iff/IffForegroundRadioService.java`

- [ ] **Step 1: Find existing approach activation**

Search:

`rg -n "approachActive|iff_approach|activateApproach|я подхожу|approach" app/src/main/java/net/afterday/compas`

Expected: identify the click handler that currently sets `approachActive`.

- [ ] **Step 2: Call new core approach activation**

In the click handler for `iff_approach`, add:

```java
IffForegroundRadioService.activateApproach();
```

Keep existing UI feedback until the new map is confirmed.

- [ ] **Step 3: Publish approach on the next service tick**

Use the existing one-second foreground-service tick to publish the approach flag. Keep `activateApproach()` static and small:

```java
public static void activateApproach() {
    APPROACH.activate(SystemClock.elapsedRealtime());
}
```

The next call to `rememberLocation(...)` or `recordAutoFieldCheckSnapshot(...)` must merge local state with:

```java
APPROACH.isActive(SystemClock.elapsedRealtime())
```

This keeps UI responsive and satisfies the <=2 second update cadence.

- [ ] **Step 4: Verify build**

Run:

```powershell
scripts\test-iff-coordinate-core.ps1
gradle :app:assembleDebug
```

Expected: tests pass, build succeeds.

## Task 9: Field Prep And Verification

**Files:**
- No source changes unless verification exposes a build issue.

- [ ] **Step 1: Install APK on all connected devices**

Run:

```powershell
adb devices
adb -s 83efb856 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s e089985a install -r app\build\outputs\apk\debug\app-debug.apk
adb -s R3CT20C8A8N install -r app\build\outputs\apk\debug\app-debug.apk
```

Expected: three `Success` install results.

- [ ] **Step 2: Clear logs**

Run:

```powershell
adb -s 83efb856 shell rm -f /sdcard/Android/data/net.afterday.compas/files/diagnostics/*.log
adb -s e089985a shell rm -f /sdcard/Android/data/net.afterday.compas/files/diagnostics/*.log
adb -s R3CT20C8A8N shell rm -f /sdcard/Android/data/net.afterday.compas/files/diagnostics/*.log
adb -s 83efb856 logcat -c
adb -s e089985a logcat -c
adb -s R3CT20C8A8N logcat -c
```

- [ ] **Step 3: Launch IFF**

Run:

```powershell
adb -s 83efb856 shell am start -n net.afterday.compas/.IffActivity
adb -s e089985a shell am start -n net.afterday.compas/.IffActivity
adb -s R3CT20C8A8N shell am start -n net.afterday.compas/.IffActivity
```

- [ ] **Step 4: Confirm diagnostics**

After 15 seconds, read latest logs and check:

```powershell
adb -s 83efb856 shell "grep 'participantMapStatus' /sdcard/Android/data/net.afterday.compas/files/diagnostics/field-radio-*.log | tail -n 3"
adb -s e089985a shell "grep 'participantMapStatus' /sdcard/Android/data/net.afterday.compas/files/diagnostics/field-radio-*.log | tail -n 3"
adb -s R3CT20C8A8N shell "grep 'participantMapStatus' /sdcard/Android/data/net.afterday.compas/files/diagnostics/field-radio-*.log | tail -n 3"
```

Expected:

- `snapshotIntervalMs=1000` still appears in auto snapshots.
- `participantMapStatus` appears.
- `mode=SPATIAL` when local GPS and at least one remote coordinate are usable.
- Pressing "я подхожу" on one phone makes that participant log `approach=true` on other phones within 2 seconds.

## Self-Review Notes

- Spec coverage: participant state, coordinate exchange, map behavior, approach semantics, transport-agnostic payloads, diagnostics, migration, and field validation are each mapped to tasks.
- Scope: first slice does not include internet relay and does not delete legacy IFF code.
- Risk: `IffForegroundRadioService.activateApproach()` is published on the next foreground-service tick rather than synchronously; this matches the <=2 second update cadence.
- Completion scan: no unfinished implementation markers are intentionally left in implementation steps.
