package net.afterday.compas.iff;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import java.util.ArrayList;
import java.util.List;
import net.afterday.compas.IffActivity;
import net.afterday.compas.R;
import net.afterday.compas.logging.FieldDiagnosticLog;

public final class IffForegroundRadioService extends Service {
    private static final String ACTION_START = "net.afterday.compas.iff.START_RADIO";
    private static final String ACTION_STOP = "net.afterday.compas.iff.STOP_RADIO";
    private static final String EXTRA_LOCAL_PLAYER_ID = "localPlayerId";
    private static final String EXTRA_LOCAL_DISPLAY_NAME = "localDisplayName";
    private static final String CHANNEL_ID = "compass_iff_radio";
    private static final int NOTIFICATION_ID = 2701;
    private static final int WIFI_FINGERPRINT_MAX_ENTRIES = 8;
    private static final long DISTANCE_WINDOW_MS = 6000L;
    private static final long ACCEPTED_LOCATION_FRESH_MS = 15000L;
    private static final long ACCEPTED_LOCATION_STALE_MS = 120000L;
    private static final String FUSED_PROVIDER = "fused";
    private static final Object LOCK = new Object();
    private static final String[] FIELD_PLAYER_IDS = new String[] {"petya", "vasya", "zhenya"};
    private static final IffParticipantStore PARTICIPANTS = new IffParticipantStore("");
    private static final IffParticipantDisplayNames PARTICIPANT_DISPLAY_NAMES =
            new IffParticipantDisplayNames();
    private static final IffApproachState APPROACH = new IffApproachState(30000L);

    private static boolean running;
    private static String localPlayerId = "";
    private static String localDisplayName = "";
    private static String lastStatus = "service idle";
    private long lastAutoFieldCheckElapsedMs;
    private Location latestLocation;
    private LocationManager activeLocationManager;
    private final IffGpsStabilizer gpsStabilizer = new IffGpsStabilizer();
    private final IffOperatorFieldSnapshotStore operatorFieldSnapshotStore =
            new IffOperatorFieldSnapshotStore();

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            rememberLocation(location, "location_update");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };
    private final LocationListener singleLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            rememberLocation(location, "single_location_update");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private final Handler handler = new Handler();
    private final Runnable witnessTransitionLogger = new Runnable() {
        @Override
        public void run() {
            IffRadioWitnessStore.logFreshnessTransitions("foreground_service_tick");
            long now = SystemClock.elapsedRealtime();
            if (IffAutoFieldCheckSnapshot.shouldRecord(now, lastAutoFieldCheckElapsedMs)) {
                lastAutoFieldCheckElapsedMs = now;
                recordAutoFieldCheckSnapshot("foreground_service_tick");
            }
            handler.postDelayed(this, IffAutoFieldCheckSnapshot.INTERVAL_MS);
        }
    };

    public static void start(Context context, String localPlayerId) {
        start(context, localPlayerId, localPlayerId);
    }

    public static void start(Context context, String localPlayerId, String localDisplayName) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, IffForegroundRadioService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_LOCAL_PLAYER_ID, safe(localPlayerId));
        intent.putExtra(EXTRA_LOCAL_DISPLAY_NAME, safe(localDisplayName));
        if (Build.VERSION.SDK_INT >= 26) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, IffForegroundRadioService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    public static String compactStatus() {
        synchronized (LOCK) {
            return "iff radio service " + (running ? "on" : "off")
                    + " local=" + localPlayerId
                    + " name=\"" + clean(localDisplayName) + "\""
                    + " " + lastStatus;
        }
    }

    public static IffParticipantMapModel.Snapshot participantMapSnapshot(String requestedLocalPlayerId) {
        String snapshotLocalPlayerId = safe(requestedLocalPlayerId);
        if (snapshotLocalPlayerId.trim().length() == 0) {
            synchronized (LOCK) {
                snapshotLocalPlayerId = localPlayerId;
            }
        }
        return IffParticipantMapModel.from(
                PARTICIPANTS,
                safe(snapshotLocalPlayerId),
                SystemClock.elapsedRealtime());
    }

    public static List<IffParticipantState> participantStatesSnapshot() {
        return PARTICIPANTS.snapshotAll();
    }

    public static void activateApproach() {
        APPROACH.activate(SystemClock.elapsedRealtime());
    }

    public static void clearApproach() {
        APPROACH.clear();
    }

    public static void mergeParticipantState(IffParticipantState state) {
        if (state == null) {
            return;
        }
        PARTICIPANT_DISPLAY_NAMES.remember(state.playerId, state.displayName);
        String currentLocalPlayerId;
        synchronized (LOCK) {
            currentLocalPlayerId = localPlayerId;
        }
        if (state.playerId.equals(safe(currentLocalPlayerId)) && state.hopCount > 0) {
            return;
        }
        PARTICIPANTS.merge(state, SystemClock.elapsedRealtime());
    }

    public static void rememberParticipantDisplayName(String playerId, String displayName) {
        PARTICIPANT_DISPLAY_NAMES.remember(playerId, displayName);
    }

    public static String participantDisplayNameFor(String playerId) {
        return PARTICIPANT_DISPLAY_NAMES.displayNameFor(playerId);
    }

    public static String coordinateMessageForBroadcast(String requestedLocalPlayerId, long sequence) {
        long now = SystemClock.elapsedRealtime();
        String senderPlayerId = safe(requestedLocalPlayerId);
        if (senderPlayerId.length() == 0) {
            synchronized (LOCK) {
                senderPlayerId = safe(localPlayerId);
            }
        }
        if (senderPlayerId.length() == 0) {
            return "";
        }
        List<IffParticipantState> sourceStates = PARTICIPANTS.snapshot(now);
        List<IffParticipantState> broadcastStates = new ArrayList<>();
        for (int i = 0; i < sourceStates.size(); i++) {
            IffParticipantState state = sourceStates.get(i);
            if (state == null) {
                continue;
            }
            if (senderPlayerId.equals(state.playerId)) {
                broadcastStates.add(state);
                continue;
            }
            IffParticipantState relayed = IffParticipantState.create(
                    state.playerId,
                    senderPlayerId,
                    state.displayName,
                    state.latitude,
                    state.longitude,
                    state.accuracyMeters,
                    state.locationTimeMillis,
                    now,
                    state.hopCount + 1,
                    state.rssiDbm,
                    state.approachActive);
            if (relayed != null) {
                broadcastStates.add(relayed);
            }
        }
        return IffCoordinateMessage.encode(senderPlayerId, sequence, broadcastStates);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : safe(intent.getAction());
        if (ACTION_STOP.equals(action)) {
            stopRadio("notification_stop");
            stopSelf();
            return START_NOT_STICKY;
        }

        String nextLocalPlayerId = intent == null ? "" : safe(intent.getStringExtra(EXTRA_LOCAL_PLAYER_ID));
        String nextLocalDisplayName = intent == null ? "" : safe(intent.getStringExtra(EXTRA_LOCAL_DISPLAY_NAME));
        startForegroundNotification(nextLocalPlayerId);
        startRadio(nextLocalPlayerId, nextLocalDisplayName);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopRadio("foreground_service_destroy");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startRadio(String nextLocalPlayerId, String nextLocalDisplayName) {
        FieldDiagnosticLog.start(this);
        synchronized (LOCK) {
            running = true;
            localPlayerId = safe(nextLocalPlayerId);
            localDisplayName = normalizedDisplayName(nextLocalDisplayName, localPlayerId);
            lastStatus = "foreground connectedDevice";
        }
        PARTICIPANT_DISPLAY_NAMES.remember(localPlayerId, localDisplayName);
        FieldDiagnosticLog.event("IFF_DIAG", "event=iff_radio_service_start"
                + " lifecycle=FOREGROUND_SERVICE_CONNECTED_DEVICE"
                + " localPlayerId=" + localPlayerId
                + " localDisplayName=\"" + clean(localDisplayName) + "\""
                + " policy=\"" + clean(IffRadioWitnessStore.freshnessPolicyLabel()) + "\"");
        IffFieldRunSummary.reset(localPlayerId);
        startLocationUpdates();
        IffBleFieldRadio.startFromForegroundService(this, localPlayerId, localDisplayName);
        IffWifiDirectDiscoveryTransport.start(this, localPlayerId, localDisplayName);
        lastAutoFieldCheckElapsedMs = 0L;
        handler.removeCallbacks(witnessTransitionLogger);
        handler.post(witnessTransitionLogger);
    }

    private void stopRadio(String reason) {
        handler.removeCallbacks(witnessTransitionLogger);
        stopLocationUpdates(reason);
        IffBleFieldRadio.stop(reason);
        IffWifiDirectDiscoveryTransport.stop();
        synchronized (LOCK) {
            running = false;
            lastStatus = "stopped " + safe(reason);
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=iff_radio_service_stop"
                + " reason=" + safe(reason)
                + " lifecycle=FOREGROUND_SERVICE_CONNECTED_DEVICE");
    }

    private void recordAutoFieldCheckSnapshot(String source) {
        String currentLocalPlayerId;
        synchronized (LOCK) {
            currentLocalPlayerId = localPlayerId;
        }
        IffRadioWitnessStore.RssiWindowSnapshot sideA =
                IffRadioWitnessStore.getRssiWindow("vasya", IffOfficeProximityVerdict.WINDOW_MS);
        IffRadioWitnessStore.RssiWindowSnapshot sideB =
                IffRadioWitnessStore.getRssiWindow("zhenya", IffOfficeProximityVerdict.WINDOW_MS);
        IffOfficeProximityVerdict.Snapshot officeVerdict = IffOfficeProximityVerdict.evaluate(
                currentLocalPlayerId,
                sideA.asOfficeSample(),
                sideB.asOfficeSample());
        DistanceTargetSnapshot distanceTarget = preferredDistanceTarget(currentLocalPlayerId);
        Location rawLocalGpsLocation = readBestLocation();
        boolean localGpsOutlier = isGpsOutlier(rawLocalGpsLocation);
        Location localGpsLocation = localGpsOutlier ? null : rawLocalGpsLocation;
        if (localGpsOutlier) {
            quarantineLocalGps(rawLocalGpsLocation, currentLocalPlayerId, "auto_field_check");
        }
        IffBleFieldRadio.updateLocalGps(localGpsLocation);
        IffWifiDirectDiscoveryTransport.updateLocalGps(localGpsLocation);
        IffRemoteWitnessReport remoteGpsReport = IffRemoteWitnessStore.getFreshGpsReportFor(distanceTarget.playerId);
        long participantNowMillis = SystemClock.elapsedRealtime();
        mergeLocalParticipantState(localGpsLocation, currentLocalPlayerId, participantNowMillis);
        mergeFreshRemoteGpsReports(currentLocalPlayerId, participantNowMillis);
        IffGpsSnapshot gpsSnapshot = localGpsOutlier
                ? IffGpsSnapshot.outlier(gpsAccuracyValue(rawLocalGpsLocation))
                : readGpsSnapshot(localGpsLocation, remoteGpsReport, distanceTarget.trend.distanceClass);
        WifiFingerprintSnapshot wifiSnapshot = readWifiFingerprintSnapshot(currentLocalPlayerId);
        IffFieldLocatorSnapshot locatorSnapshot = IffFieldLocatorSnapshot.from(
                IffWifiTargetObservationStore.snapshot(),
                distanceTarget.trend,
                gpsSnapshot);
        String wifiTargetStatus = IffWifiTargetObservationStore.compactStatus();
        IffFieldMapSnapshot rawFieldMapSnapshot = IffFieldMapSnapshot.from(locatorSnapshot, wifiTargetStatus);
        IffFieldMapSnapshot operatorFieldMapSnapshot = operatorFieldSnapshotStore.update(
                rawFieldMapSnapshot,
                SystemClock.elapsedRealtime());
        IffParticipantMapModel.Snapshot participantMap = participantMapSnapshot(currentLocalPlayerId);
        String officeRole = IffAutoFieldCheckSnapshot.officeTestRole(currentLocalPlayerId);

        IffFieldRunSummary.record(new IffFieldRunSummary.Check(
                currentLocalPlayerId,
                officeRole,
                officeVerdict.label,
                distanceTarget.trend.distanceClass,
                distanceTarget.trend.movementTrend,
                gpsSnapshot.status,
                wifiSnapshot.freshness));

        FieldDiagnosticLog.event("IFF_DIAG", "event=auto_field_check"
                + " source=" + safe(source)
                + " snapshotIntervalMs=" + IffAutoFieldCheckSnapshot.INTERVAL_MS
                + " localDevicePlayerId=" + safe(currentLocalPlayerId)
                + " officeRole=" + officeRole
                + " officeProximityVerdict=" + officeVerdict.label
                + " officeProximityDeltaDb=" + officeVerdict.deltaDb
                + " officeProximityReason=\"" + clean(officeVerdict.reason) + "\""
                + " officeProximityA=\"" + clean(officeSampleLabel(sideA)) + "\""
                + " officeProximityB=\"" + clean(officeSampleLabel(sideB)) + "\""
                + " distanceTargetPlayerId=" + distanceTarget.playerId
                + " distanceClass=" + distanceTarget.trend.distanceClass
                + " distanceConfidence=" + distanceTarget.trend.distanceConfidence
                + " movementTrend=" + distanceTarget.trend.movementTrend
                + " movementConfidence=" + distanceTarget.trend.movementConfidence
                + " movementRssiDeltaDb=" + distanceTarget.trend.movementRssiDeltaDb
                + " gpsStatus=" + gpsSnapshot.status
                + " gpsAccuracyM=" + gpsSnapshot.fieldValue(gpsSnapshot.accuracyM)
                + " gpsDistanceM=" + gpsSnapshot.fieldValue(gpsSnapshot.distanceM)
                + " gpsBearingDeg=" + gpsSnapshot.fieldValue(gpsSnapshot.bearingDeg)
                + " remoteGpsSource=" + (remoteGpsReport == null ? "none" : safe(remoteGpsReport.sourcePlayerId))
                + " gpsLocalProvider=" + safe(rawLocalGpsLocation == null ? "" : rawLocalGpsLocation.getProvider())
                + " gpsLocalCluster=" + gpsCluster(rawLocalGpsLocation)
                + " gpsLocalLatE7=" + gpsLatE7(rawLocalGpsLocation)
                + " gpsLocalLonE7=" + gpsLonE7(rawLocalGpsLocation)
                + " gpsLocalAgeMs=" + gpsAgeMs(rawLocalGpsLocation)
                + " gpsLocalAccuracyM=" + gpsAccuracyM(rawLocalGpsLocation)
                + " gpsRemoteCluster=" + gpsCluster(remoteGpsReport)
                + " gpsRemoteLatE7=" + gpsLatE7(remoteGpsReport)
                + " gpsRemoteLonE7=" + gpsLonE7(remoteGpsReport)
                + " gpsRemoteAgeMs=" + gpsAgeMs(remoteGpsReport)
                + " gpsRemoteAccuracyM=" + gpsAccuracyM(remoteGpsReport)
                + " gpsRawDistanceM=" + gpsRawDistanceM(localGpsLocation, remoteGpsReport)
                + " gpsRawBearingDeg=" + gpsRawBearingDeg(localGpsLocation, remoteGpsReport)
                + " fieldRadioStatus=\"" + clean(IffBleFieldRadio.compactStatus()) + "\""
                + " fieldRadioPolicy=\"" + clean(IffBleFieldRadio.lifecycleStatus()) + "\""
                + " fieldLocatorStatus=\"" + clean(locatorSnapshot.compact()) + "\""
                + " operatorFieldMapStatus=\"" + clean(operatorFieldMapStatus(operatorFieldMapSnapshot)) + "\""
                + " participantMapStatus=\"" + clean(participantMapStatus(participantMap)) + "\""
                + " wifiDirectStatus=\"" + clean(IffWifiDirectDiscoveryTransport.compactStatus()) + "\""
                + " wifiTargetStatus=\"" + clean(wifiTargetStatus) + "\""
                + " wifiFingerprintStatus=" + safe(wifiSnapshot.status)
                + " wifiRefreshRequested=" + wifiSnapshot.refreshRequested
                + " wifiRefreshAccepted=" + wifiSnapshot.refreshAccepted
                + " wifiFreshness=" + safe(wifiSnapshot.freshness)
                + " wifiFreshAgeMs=" + wifiSnapshot.freshAgeMs
                + " wifiFingerprint=\"" + clean(wifiSnapshot.fingerprint) + "\""
                + " transportStatus=\"" + clean(IffUdpWitnessTransport.compactStatus()) + "\"");
    }

    private static String operatorFieldMapStatus(IffFieldMapSnapshot snapshot) {
        if (snapshot == null) {
            return "source=NONE readiness=NO_ANCHORS distance=na clock=na visible=false directionKnown=false";
        }
        return "source=" + safe(snapshot.source)
                + " readiness=" + safe(snapshot.readiness)
                + " distance=" + (snapshot.distanceBucketM > 0 ? snapshot.distanceBucketM + "m" : "na")
                + " clock=" + safe(snapshot.clockDirection)
                + " visible=" + snapshot.targetVisible
                + " directionKnown=" + snapshot.directionKnown
                + " statusLine=" + clean(snapshot.statusLine);
    }

    private static String participantMapStatus(IffParticipantMapModel.Snapshot snapshot) {
        if (snapshot == null) {
            return "mode=NONE visibleCount=0 hiddenCount=0 reason=missing points=[]";
        }
        StringBuilder status = new StringBuilder();
        status.append("mode=").append(safe(snapshot.mode))
                .append(" visibleCount=").append(snapshot.points == null ? 0 : snapshot.points.size())
                .append(" hiddenCount=").append(snapshot.hiddenCount)
                .append(" reason=").append(clean(snapshot.reason))
                .append(" points=[");
        if (snapshot.points != null) {
            for (int i = 0; i < snapshot.points.size(); i++) {
                if (i > 0) {
                    status.append("; ");
                }
                IffParticipantMapModel.Point point = snapshot.points.get(i);
                status.append("playerId=").append(safe(point.playerId))
                        .append(" displayName=").append(clean(point.displayName))
                        .append(" distanceM=").append(point.distanceM)
                        .append(" bearingDeg=").append(point.bearingDeg)
                        .append(" ageMs=").append(point.ageMs)
                        .append(" accuracyMeters=").append(participantAccuracy(point.accuracyMeters))
                        .append(" sourcePlayerId=").append(safe(point.sourcePlayerId))
                        .append(" hopCount=").append(point.hopCount)
                        .append(" rssiDbm=").append(point.rssiDbm)
                        .append(" approachActive=").append(point.approachActive);
            }
        }
        status.append("]");
        return status.toString();
    }

    private static String participantAccuracy(float accuracyMeters) {
        if (Float.isNaN(accuracyMeters) || Float.isInfinite(accuracyMeters)) {
            return "na";
        }
        return String.valueOf(Math.round(accuracyMeters));
    }

    private DistanceTargetSnapshot officeDistanceTrend() {
        IffRadioWitnessStore.RssiWindowSnapshot currentA =
                IffRadioWitnessStore.getRssiWindow("vasya", DISTANCE_WINDOW_MS);
        IffRadioWitnessStore.RssiWindowSnapshot currentB =
                IffRadioWitnessStore.getRssiWindow("zhenya", DISTANCE_WINDOW_MS);
        IffRadioWitnessStore.RssiWindowSnapshot current = strongestUsable(currentA, currentB);
        if (current == null) {
            return new DistanceTargetSnapshot("", IffDistanceTrend.evaluate(null, null));
        }
        IffRadioWitnessStore.RssiWindowSnapshot previous =
                IffRadioWitnessStore.getPreviousRssiWindow(current.playerId, DISTANCE_WINDOW_MS);
        return new DistanceTargetSnapshot(
                current.playerId,
                IffDistanceTrend.evaluate(current.asDistanceSample(), previous.asDistanceSample()));
    }

    private DistanceTargetSnapshot preferredDistanceTarget(String currentLocalPlayerId) {
        if (!IffWifiTargetObservationStore.TARGET_PLAYER_ID.equals(currentLocalPlayerId)) {
            DistanceTargetSnapshot target = radioDistanceFor(IffWifiTargetObservationStore.TARGET_PLAYER_ID);
            if (!"LOST".equals(target.trend.distanceClass)) {
                return target;
            }
        }
        return officeDistanceTrend();
    }

    private DistanceTargetSnapshot radioDistanceFor(String playerId) {
        IffRadioWitnessStore.RssiWindowSnapshot current =
                IffRadioWitnessStore.getRssiWindow(playerId, DISTANCE_WINDOW_MS);
        if (current == null || !current.fresh || current.validCount <= 0) {
            return new DistanceTargetSnapshot(safe(playerId), IffDistanceTrend.evaluate(null, null));
        }
        IffRadioWitnessStore.RssiWindowSnapshot previous =
                IffRadioWitnessStore.getPreviousRssiWindow(playerId, DISTANCE_WINDOW_MS);
        return new DistanceTargetSnapshot(
                safe(playerId),
                IffDistanceTrend.evaluate(current.asDistanceSample(), previous.asDistanceSample()));
    }

    private IffRadioWitnessStore.RssiWindowSnapshot strongestUsable(
            IffRadioWitnessStore.RssiWindowSnapshot left,
            IffRadioWitnessStore.RssiWindowSnapshot right) {
        boolean leftUsable = left != null && left.fresh && left.validCount > 0;
        boolean rightUsable = right != null && right.fresh && right.validCount > 0;
        if (!leftUsable && !rightUsable) {
            return null;
        }
        if (leftUsable && !rightUsable) {
            return left;
        }
        if (rightUsable && !leftUsable) {
            return right;
        }
        return left.averageRssi >= right.averageRssi ? left : right;
    }

    private IffGpsSnapshot readGpsSnapshot(
            Location localGpsLocation,
            IffRemoteWitnessReport remoteGpsReport,
            String radioDistanceClass) {
        if (localGpsLocation != null && remoteGpsReport != null && remoteGpsReport.hasGpsFix()) {
            long localAgeMs = Math.max(0L, System.currentTimeMillis() - localGpsLocation.getTime());
            return IffGpsSnapshot.fromPair(
                    localAgeMs,
                    localGpsLocation.hasAccuracy(),
                    localGpsLocation.hasAccuracy() ? localGpsLocation.getAccuracy() : -1.0f,
                    localGpsLocation.getLatitude(),
                    localGpsLocation.getLongitude(),
                    remoteGpsReport.gpsAgeMs(),
                    true,
                    remoteGpsReport.gpsAccuracyM,
                    remoteGpsReport.gpsLatitude(),
                    remoteGpsReport.gpsLongitude(),
                    radioDistanceClass);
        }
        return readGpsSnapshot(localGpsLocation);
    }

    private IffGpsSnapshot readGpsSnapshot(Location localGpsLocation) {
        if (localGpsLocation == null) {
            return IffGpsSnapshot.unavailable();
        }
        long ageMs = Math.max(0L, System.currentTimeMillis() - localGpsLocation.getTime());
        return IffGpsSnapshot.from(
                ageMs,
                localGpsLocation.hasAccuracy(),
                localGpsLocation.hasAccuracy() ? localGpsLocation.getAccuracy() : -1.0f,
                localGpsLocation.hasBearing(),
                localGpsLocation.hasBearing() ? localGpsLocation.getBearing() : -1.0f);
    }

    @Nullable
    private Location readBestLocation() {
        if (!hasLocationPermission()) {
            return null;
        }
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }
        return bestAvailableLocation(locationManager);
    }

    @Nullable
    private Location bestAvailableLocation(LocationManager locationManager) {
        Location best = bestLastKnownLocation(locationManager);
        Location latest;
        synchronized (LOCK) {
            latest = latestLocation == null ? null : new Location(latestLocation);
        }
        long nowWallMs = System.currentTimeMillis();
        if (latest != null
                && IffLocationFreshness.usableAgeMs(nowWallMs, latest.getTime(), ACCEPTED_LOCATION_STALE_MS) >= 0L) {
            return latest;
        }
        latest = null;
        if (best != null
                && IffLocationFreshness.usableAgeMs(nowWallMs, best.getTime(), ACCEPTED_LOCATION_STALE_MS) < 0L) {
            best = null;
        }
        if (best == null) {
            return null;
        }
        if (latest == null) {
            return best;
        }
        return latest.getTime() >= best.getTime() ? latest : best;
    }

    private void startLocationUpdates() {
        stopLocationUpdates("restart_location_updates");
        if (!hasLocationPermission()) {
            FieldDiagnosticLog.event("IFF_DIAG", "event=gps_updates_start requested=false reason=missing_permission");
            return;
        }
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            FieldDiagnosticLog.event("IFF_DIAG", "event=gps_updates_start requested=false reason=no_location_manager");
            return;
        }
        activeLocationManager = locationManager;
        rememberLocation(bestLastKnownLocation(locationManager), "last_known");
        int requested = 0;
        if (requestLocationProvider(locationManager, LocationManager.GPS_PROVIDER)) {
            requested++;
        }
        if (requestLocationProvider(locationManager, FUSED_PROVIDER)) {
            requested++;
        }
        if (requestLocationProvider(locationManager, LocationManager.NETWORK_PROVIDER)) {
            requested++;
        }
        if (requestLocationProvider(locationManager, LocationManager.PASSIVE_PROVIDER)) {
            requested++;
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=gps_updates_start requested=true providers=" + requested);
    }

    private boolean requestLocationProvider(LocationManager locationManager, String provider) {
        try {
            if (!locationManager.isProviderEnabled(provider)) {
                return false;
            }
            locationManager.requestLocationUpdates(provider, 1000L, 0.0f, locationListener);
            boolean singleRequested = requestSingleLocationUpdate(locationManager, provider);
            FieldDiagnosticLog.event("IFF_DIAG", "event=gps_provider_request provider="
                    + safe(provider) + " accepted=true single=" + singleRequested);
            return true;
        } catch (SecurityException e) {
            FieldDiagnosticLog.event("IFF_DIAG", "event=gps_provider_request provider="
                    + safe(provider) + " accepted=false denied=true");
            return false;
        } catch (Exception e) {
            FieldDiagnosticLog.event("IFF_DIAG", "event=gps_provider_request provider="
                    + safe(provider) + " accepted=false error=\"" + clean(e.getClass().getSimpleName()) + "\"");
            return false;
        }
    }

    private boolean requestSingleLocationUpdate(LocationManager locationManager, String provider) {
        try {
            locationManager.requestSingleUpdate(provider, singleLocationListener, getMainLooper());
            return true;
        } catch (SecurityException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void stopLocationUpdates(String reason) {
        LocationManager locationManager = activeLocationManager;
        activeLocationManager = null;
        if (locationManager == null) {
            return;
        }
        try {
            locationManager.removeUpdates(locationListener);
            locationManager.removeUpdates(singleLocationListener);
            FieldDiagnosticLog.event("IFF_DIAG", "event=gps_updates_stop reason=" + safe(reason));
        } catch (Exception e) {
            FieldDiagnosticLog.event("IFF_DIAG", "event=gps_updates_stop reason=" + safe(reason)
                    + " error=\"" + clean(e.getClass().getSimpleName()) + "\"");
        }
    }

    private static void mergeLocalParticipantState(
            Location location,
            String currentLocalPlayerId,
            long receivedTimeMillis) {
        if (location == null || safe(currentLocalPlayerId).length() == 0) {
            return;
        }
        String currentLocalDisplayName;
        synchronized (LOCK) {
            currentLocalDisplayName = localDisplayName;
        }
        long wallAgeMs = IffLocationFreshness.usableAgeMs(
                System.currentTimeMillis(),
                location.getTime(),
                ACCEPTED_LOCATION_STALE_MS);
        if (wallAgeMs < 0L) {
            return;
        }
        long stateReceivedTimeMillis = Math.max(0L, receivedTimeMillis - wallAgeMs);
        float accuracyMeters = normalizedAccuracyMeters(location.hasAccuracy() ? location.getAccuracy() : 100.0f);
        PARTICIPANTS.merge(
                IffParticipantState.create(
                        currentLocalPlayerId,
                        currentLocalPlayerId,
                        normalizedDisplayName(currentLocalDisplayName, currentLocalPlayerId),
                        location.getLatitude(),
                        location.getLongitude(),
                        accuracyMeters,
                        location.getTime(),
                        stateReceivedTimeMillis,
                        0,
                        Integer.MIN_VALUE,
                        APPROACH.isActive(receivedTimeMillis)),
                receivedTimeMillis);
    }

    private static void mergeFreshRemoteGpsReports(String currentLocalPlayerId, long receivedTimeMillis) {
        for (int i = 0; i < FIELD_PLAYER_IDS.length; i++) {
            String playerId = FIELD_PLAYER_IDS[i];
            if (playerId.equals(safe(currentLocalPlayerId))) {
                continue;
            }
            IffRemoteWitnessReport report = IffRemoteWitnessStore.getFreshGpsReportFor(playerId);
            if (report == null || !report.hasGpsFix()) {
                continue;
            }
            if (!IffGpsSanity.isPlausibleCoordinate(report.gpsLatitude(), report.gpsLongitude())) {
                FieldDiagnosticLog.event("IFF_DIAG", "event=remote_gps_rejected"
                        + " playerId=" + safe(playerId)
                        + " sourcePlayerId=" + safe(report.sourcePlayerId)
                        + " reason=gps_outlier"
                        + " latE7=" + report.gpsLatE7
                        + " lonE7=" + report.gpsLonE7
                        + " accuracyM=" + report.gpsAccuracyM);
                continue;
            }
            int hopCount = report.sourcePlayerId.endsWith(report.targetPlayerId) ? 0 : 1;
            PARTICIPANTS.merge(
                    IffParticipantState.create(
                            report.targetPlayerId,
                            report.sourcePlayerId,
                            PARTICIPANT_DISPLAY_NAMES.displayNameFor(report.targetPlayerId),
                            report.gpsLatitude(),
                            report.gpsLongitude(),
                            normalizedAccuracyMeters(report.gpsAccuracyM),
                            report.gpsObservedElapsedMs,
                            receivedTimeMillis,
                            hopCount,
                            report.rssi,
                            false),
                    receivedTimeMillis);
        }
    }

    private static float normalizedAccuracyMeters(float accuracyMeters) {
        if (Float.isNaN(accuracyMeters) || Float.isInfinite(accuracyMeters) || accuracyMeters <= 0.0f) {
            return 100.0f;
        }
        return Math.max(1.0f, accuracyMeters);
    }

    private void rememberLocation(Location location, String source) {
        if (location == null) {
            return;
        }
        long wallAgeMs = IffLocationFreshness.usableAgeMs(
                System.currentTimeMillis(),
                location.getTime(),
                ACCEPTED_LOCATION_STALE_MS);
        if (wallAgeMs < 0L) {
            FieldDiagnosticLog.event("IFF_DIAG", "event=gps_location_rejected"
                    + " source=" + safe(source)
                    + " provider=" + safe(location.getProvider())
                    + " reason=rejected_stale"
                    + " ageMs=na"
                    + " accuracyM=" + (location.hasAccuracy() ? Math.round(location.getAccuracy()) : "na")
                    + " latE7=" + gpsLatE7(location)
                    + " lonE7=" + gpsLonE7(location)
                    + " cluster=" + gpsCluster(location));
            return;
        }
        if (isGpsOutlier(location)) {
            String acceptedLocalPlayerId;
            synchronized (LOCK) {
                acceptedLocalPlayerId = localPlayerId;
            }
            quarantineLocalGps(location, acceptedLocalPlayerId, source);
            return;
        }
        String acceptedLocalPlayerId;
        synchronized (LOCK) {
            if (latestLocation != null && latestLocation.getTime() > location.getTime()) {
                return;
            }
            IffGpsStabilizer.Decision decision = gpsStabilizer.evaluate(
                    location.getLatitude(),
                    location.getLongitude(),
                    location.hasAccuracy(),
                    location.hasAccuracy() ? location.getAccuracy() : -1.0f,
                    location.getTime(),
                    location.getProvider());
            if (!decision.accepted) {
                long rejectedAgeMs = Math.max(0L, System.currentTimeMillis() - location.getTime());
                FieldDiagnosticLog.event("IFF_DIAG", "event=gps_location_rejected"
                        + " source=" + safe(source)
                        + " provider=" + safe(location.getProvider())
                        + " reason=" + safe(decision.reason)
                        + " jumpDistanceM=" + decision.jumpDistanceM
                        + " ageMs=" + rejectedAgeMs
                        + " accuracyM=" + (location.hasAccuracy() ? Math.round(location.getAccuracy()) : "na")
                        + " latE7=" + gpsLatE7(location)
                        + " lonE7=" + gpsLonE7(location)
                        + " cluster=" + gpsCluster(location));
                return;
            }
            latestLocation = new Location(location);
            acceptedLocalPlayerId = localPlayerId;
        }
        long receivedTimeMillis = SystemClock.elapsedRealtime();
        mergeLocalParticipantState(location, acceptedLocalPlayerId, receivedTimeMillis);
        IffBleFieldRadio.updateLocalGps(location);
        IffWifiDirectDiscoveryTransport.updateLocalGps(location);
        long ageMs = Math.max(0L, System.currentTimeMillis() - location.getTime());
        FieldDiagnosticLog.event("IFF_DIAG", "event=gps_location_update"
                + " source=" + safe(source)
                + " provider=" + safe(location.getProvider())
                + " ageMs=" + ageMs
                + " accuracyM=" + (location.hasAccuracy() ? Math.round(location.getAccuracy()) : "na")
                + " bearingDeg=" + (location.hasBearing() ? Math.round(location.getBearing()) : "na")
                + " latE7=" + gpsLatE7(location)
                + " lonE7=" + gpsLonE7(location)
                + " cluster=" + gpsCluster(location));
    }

    private static boolean isGpsOutlier(@Nullable Location location) {
        return location != null
                && !IffGpsSanity.isPlausibleCoordinate(location.getLatitude(), location.getLongitude());
    }

    private void quarantineLocalGps(Location location, String currentLocalPlayerId, String source) {
        synchronized (LOCK) {
            latestLocation = null;
            lastStatus = "GPS OUTLIER radio-only";
        }
        PARTICIPANTS.remove(currentLocalPlayerId);
        IffBleFieldRadio.updateLocalGps(null);
        IffWifiDirectDiscoveryTransport.updateLocalGps(null);
        startForegroundNotification(currentLocalPlayerId);
        FieldDiagnosticLog.event("IFF_DIAG", "event=gps_location_rejected"
                + " source=" + safe(source)
                + " provider=" + safe(location == null ? "" : location.getProvider())
                + " reason=gps_outlier_null_island"
                + " ageMs=" + gpsAgeMs(location)
                + " accuracyM=" + gpsAccuracyM(location)
                + " latE7=" + gpsLatE7(location)
                + " lonE7=" + gpsLonE7(location)
                + " cluster=" + gpsCluster(location)
                + " action=radio_only_no_gps_payload");
    }

    private static int gpsAccuracyValue(Location location) {
        if (location == null || !location.hasAccuracy()) {
            return -1;
        }
        return Math.round(location.getAccuracy());
    }

    @Nullable
    private Location bestLastKnownLocation(LocationManager locationManager) {
        Location fused = lastKnown(locationManager, FUSED_PROVIDER);
        Location gps = lastKnown(locationManager, LocationManager.GPS_PROVIDER);
        Location network = lastKnown(locationManager, LocationManager.NETWORK_PROVIDER);
        Location passive = lastKnown(locationManager, LocationManager.PASSIVE_PROVIDER);
        return newest(newest(fused, gps), newest(network, passive));
    }

    @Nullable
    private Location newest(@Nullable Location first, @Nullable Location second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.getTime() >= second.getTime() ? first : second;
    }

    @Nullable
    private Location lastKnown(LocationManager locationManager, String provider) {
        try {
            if (!locationManager.isProviderEnabled(provider)) {
                return null;
            }
            return locationManager.getLastKnownLocation(provider);
        } catch (SecurityException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private WifiFingerprintSnapshot readWifiFingerprintSnapshot(String currentLocalPlayerId) {
        if (!hasWifiScanPermission()) {
            return new WifiFingerprintSnapshot(
                    "missing_permission",
                    false,
                    false,
                    "empty",
                    -1L,
                    "count=0 strongest=none");
        }
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return new WifiFingerprintSnapshot(
                    "unavailable",
                    false,
                    false,
                    "empty",
                    -1L,
                    "count=0 strongest=none");
        }
        boolean refreshAccepted = requestWifiRefresh(wifiManager);
        try {
            List<ScanResult> results = wifiManager.getScanResults();
            FieldDiagnosticLog.wifiScanResults("iff_auto_after_request", refreshAccepted, results);
            recordWifiTargetObservation(currentLocalPlayerId, results);
            int resultCount = results == null ? 0 : results.size();
            long newestAgeMs = newestScanAgeMs(results);
            return new WifiFingerprintSnapshot(
                    "ok",
                    true,
                    refreshAccepted,
                    IffFieldSnapshotFormatter.wifiFreshness(resultCount, newestAgeMs),
                    newestAgeMs,
                    IffFieldSnapshotFormatter.wifiFingerprint(toWifiEntries(results), WIFI_FINGERPRINT_MAX_ENTRIES));
        } catch (SecurityException e) {
            FieldDiagnosticLog.wifi("event=scan_results source=iff_auto_after_request denied=true error=\"SecurityException\"");
            return new WifiFingerprintSnapshot(
                    "security_exception",
                    true,
                    refreshAccepted,
                    "empty",
                    -1L,
                    "count=0 strongest=none");
        } catch (Exception e) {
            FieldDiagnosticLog.wifi("event=scan_results source=iff_auto_after_request denied=true error=\""
                    + clean(e.getClass().getSimpleName()) + "\"");
            return new WifiFingerprintSnapshot(
                    "error",
                    true,
                    refreshAccepted,
                    "empty",
                    -1L,
                    "count=0 strongest=none");
        }
    }

    private void recordWifiTargetObservation(String currentLocalPlayerId, List<ScanResult> results) {
        String targetPlayerId = IffWifiTargetObservationStore.TARGET_PLAYER_ID;
        String targetSsid = IffRadioWitnessStore.expectedBeaconSsid(targetPlayerId);
        ScanResult strongest = strongestExactSsid(results, targetSsid);
        if (strongest == null) {
            IffWifiDirectDiscoveryTransport.updateTargetObservation("", 0);
            return;
        }
        IffWifiTargetObservationStore.updateLocalObservation(currentLocalPlayerId, targetPlayerId, strongest.level);
        IffWifiDirectDiscoveryTransport.updateTargetObservation(targetPlayerId, strongest.level);
        FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_target_observation"
                + " localDevicePlayerId=" + safe(currentLocalPlayerId)
                + " targetPlayerId=" + safe(targetPlayerId)
                + " ssid=\"" + clean(strongest.SSID) + "\""
                + " bssid=" + safe(strongest.BSSID)
                + " rssi=" + strongest.level
                + " frequency=" + strongest.frequency
                + " source=wifi_scan");
    }

    @Nullable
    private static ScanResult strongestExactSsid(List<ScanResult> results, String ssid) {
        if (results == null || ssid == null || ssid.length() == 0) {
            return null;
        }
        ScanResult strongest = null;
        for (ScanResult result : results) {
            if (result == null || !ssid.equals(result.SSID)) {
                continue;
            }
            if (strongest == null || result.level > strongest.level) {
                strongest = result;
            }
        }
        return strongest;
    }

    private boolean requestWifiRefresh(WifiManager wifiManager) {
        try {
            boolean accepted = wifiManager.startScan();
            FieldDiagnosticLog.wifi("event=field_refresh_request source=iff_auto force=true accepted="
                    + accepted
                    + " intervalMs=" + IffAutoFieldCheckSnapshot.INTERVAL_MS);
            return accepted;
        } catch (SecurityException e) {
            FieldDiagnosticLog.wifi("event=field_refresh_request source=iff_auto force=true accepted=false denied=true error=\"SecurityException\""
                    + " intervalMs=" + IffAutoFieldCheckSnapshot.INTERVAL_MS);
            return false;
        } catch (Exception e) {
            FieldDiagnosticLog.wifi("event=field_refresh_request source=iff_auto force=true accepted=false error=\""
                    + clean(e.getClass().getSimpleName()) + "\""
                    + " intervalMs=" + IffAutoFieldCheckSnapshot.INTERVAL_MS);
            return false;
        }
    }

    private boolean hasWifiScanPermission() {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static IffFieldSnapshotFormatter.WifiEntry[] toWifiEntries(List<ScanResult> results) {
        if (results == null || results.isEmpty()) {
            return new IffFieldSnapshotFormatter.WifiEntry[0];
        }
        IffFieldSnapshotFormatter.WifiEntry[] entries =
                new IffFieldSnapshotFormatter.WifiEntry[results.size()];
        for (int i = 0; i < results.size(); i++) {
            ScanResult result = results.get(i);
            if (result == null) {
                continue;
            }
            entries[i] = new IffFieldSnapshotFormatter.WifiEntry(
                    result.SSID,
                    result.BSSID,
                    result.level,
                    result.frequency);
        }
        return entries;
    }

    private static long newestScanAgeMs(List<ScanResult> results) {
        if (results == null || results.isEmpty()) {
            return -1L;
        }
        long nowMs = SystemClock.elapsedRealtime();
        long newestAgeMs = -1L;
        for (ScanResult result : results) {
            if (result == null || result.timestamp <= 0L) {
                continue;
            }
            long ageMs = nowMs - (result.timestamp / 1000L);
            if (ageMs < 0L) {
                continue;
            }
            if (newestAgeMs < 0L || ageMs < newestAgeMs) {
                newestAgeMs = ageMs;
            }
        }
        return newestAgeMs;
    }

    private static final class WifiFingerprintSnapshot {
        final String status;
        final boolean refreshRequested;
        final boolean refreshAccepted;
        final String freshness;
        final long freshAgeMs;
        final String fingerprint;

        WifiFingerprintSnapshot(
                String status,
                boolean refreshRequested,
                boolean refreshAccepted,
                String freshness,
                long freshAgeMs,
                String fingerprint) {
            this.status = safe(status);
            this.refreshRequested = refreshRequested;
            this.refreshAccepted = refreshAccepted;
            this.freshness = safe(freshness);
            this.freshAgeMs = freshAgeMs;
            this.fingerprint = safe(fingerprint);
        }
    }

    private static final class DistanceTargetSnapshot {
        final String playerId;
        final IffDistanceTrend.Snapshot trend;

        DistanceTargetSnapshot(String playerId, IffDistanceTrend.Snapshot trend) {
            this.playerId = safe(playerId);
            this.trend = trend;
        }
    }

    private static String officeSampleLabel(IffRadioWitnessStore.RssiWindowSnapshot sample) {
        if (sample == null || (sample.validCount <= 0 && sample.outlier127Count <= 0)) {
            return "missing";
        }
        return sample.freshnessLabel()
                + " avg=" + sample.averageRssi + "dBm"
                + " n=" + sample.validCount
                + " out127=" + sample.outlier127Count
                + " newest=" + formatAge(sample.newestAgeMs);
    }

    private static String formatAge(long ageMs) {
        if (ageMs < 0L) {
            return "missing";
        }
        if (ageMs < 1000L) {
            return ageMs + "ms";
        }
        return (ageMs / 1000L) + "s";
    }

    private static String gpsCluster(Location location) {
        if (location == null) {
            return "na";
        }
        return coordinateCluster(location.getLatitude(), location.getLongitude());
    }

    private static String gpsCluster(IffRemoteWitnessReport report) {
        if (report == null || !report.hasGpsFix()) {
            return "na";
        }
        return coordinateCluster(report.gpsLatitude(), report.gpsLongitude());
    }

    private static String gpsLatE7(Location location) {
        if (location == null) {
            return "na";
        }
        return String.valueOf(coordinateE7(location.getLatitude()));
    }

    private static String gpsLonE7(Location location) {
        if (location == null) {
            return "na";
        }
        return String.valueOf(coordinateE7(location.getLongitude()));
    }

    private static String gpsLatE7(IffRemoteWitnessReport report) {
        if (report == null || !report.hasGpsFix()) {
            return "na";
        }
        return String.valueOf(report.gpsLatE7);
    }

    private static String gpsLonE7(IffRemoteWitnessReport report) {
        if (report == null || !report.hasGpsFix()) {
            return "na";
        }
        return String.valueOf(report.gpsLonE7);
    }

    private static String gpsAgeMs(Location location) {
        if (location == null) {
            return "na";
        }
        return String.valueOf(Math.max(0L, System.currentTimeMillis() - location.getTime()));
    }

    private static String gpsAgeMs(IffRemoteWitnessReport report) {
        if (report == null || !report.hasGpsFix()) {
            return "na";
        }
        return String.valueOf(report.gpsAgeMs());
    }

    private static String gpsAccuracyM(Location location) {
        if (location == null || !location.hasAccuracy()) {
            return "na";
        }
        return String.valueOf(Math.round(location.getAccuracy()));
    }

    private static String gpsAccuracyM(IffRemoteWitnessReport report) {
        if (report == null || !report.hasGpsFix()) {
            return "na";
        }
        return String.valueOf(report.gpsAccuracyM);
    }

    private static String gpsRawDistanceM(Location local, IffRemoteWitnessReport remote) {
        float[] pair = gpsRawPair(local, remote);
        if (pair == null) {
            return "na";
        }
        return String.valueOf(Math.round(pair[0]));
    }

    private static String gpsRawBearingDeg(Location local, IffRemoteWitnessReport remote) {
        float[] pair = gpsRawPair(local, remote);
        if (pair == null) {
            return "na";
        }
        float bearing = pair[1];
        if (bearing < 0.0f) {
            bearing += 360.0f;
        }
        return String.valueOf(Math.round(bearing));
    }

    private static float[] gpsRawPair(Location local, IffRemoteWitnessReport remote) {
        if (local == null || remote == null || !remote.hasGpsFix()) {
            return null;
        }
        float[] results = new float[2];
        Location.distanceBetween(
                local.getLatitude(),
                local.getLongitude(),
                remote.gpsLatitude(),
                remote.gpsLongitude(),
                results);
        return results;
    }

    private static int coordinateE7(double coordinate) {
        return (int) Math.round(coordinate * 10000000.0d);
    }

    private static String coordinateCluster(double lat, double lon) {
        return Math.round(lat * 1000.0d) + "," + Math.round(lon * 1000.0d);
    }

    private void startForegroundNotification(String playerId) {
        ensureNotificationChannel();
        Intent openIntent = new Intent(this, IffActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, IffForegroundRadioService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String status;
        synchronized (LOCK) {
            status = lastStatus;
        }
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Compass IFF radio")
                .setContentText(safe(playerId) + " / " + safe(status))
                .setContentIntent(openPendingIntent)
                .setOngoing(true)
                .addAction(R.mipmap.ic_launcher, "STOP IFF", stopPendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= 29) {
            int serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
            if (hasLocationPermission()) {
                serviceType |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
            }
            startForeground(NOTIFICATION_ID, notification, serviceType);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Compass IFF radio",
                NotificationManager.IMPORTANCE_LOW);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalizedDisplayName(String displayName, String fallbackPlayerId) {
        String trimmed = safe(displayName).trim();
        return trimmed.length() == 0 ? safe(fallbackPlayerId) : trimmed;
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }
}
