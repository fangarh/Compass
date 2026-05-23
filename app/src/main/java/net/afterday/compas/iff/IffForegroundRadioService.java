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
import java.util.List;
import net.afterday.compas.IffActivity;
import net.afterday.compas.R;
import net.afterday.compas.logging.FieldDiagnosticLog;

public final class IffForegroundRadioService extends Service {
    private static final String ACTION_START = "net.afterday.compas.iff.START_RADIO";
    private static final String ACTION_STOP = "net.afterday.compas.iff.STOP_RADIO";
    private static final String EXTRA_LOCAL_PLAYER_ID = "localPlayerId";
    private static final String CHANNEL_ID = "compass_iff_radio";
    private static final int NOTIFICATION_ID = 2701;
    private static final int WIFI_FINGERPRINT_MAX_ENTRIES = 8;
    private static final long DISTANCE_WINDOW_MS = 6000L;
    private static final long ACCEPTED_LOCATION_FRESH_MS = 15000L;
    private static final Object LOCK = new Object();

    private static boolean running;
    private static String localPlayerId = "";
    private static String lastStatus = "service idle";
    private long lastAutoFieldCheckElapsedMs;
    private Location latestLocation;
    private LocationManager activeLocationManager;
    private final IffGpsStabilizer gpsStabilizer = new IffGpsStabilizer();

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
            handler.postDelayed(this, 2000L);
        }
    };

    public static void start(Context context, String localPlayerId) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, IffForegroundRadioService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_LOCAL_PLAYER_ID, safe(localPlayerId));
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
                    + " " + lastStatus;
        }
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
        startForegroundNotification(nextLocalPlayerId);
        startRadio(nextLocalPlayerId);
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

    private void startRadio(String nextLocalPlayerId) {
        FieldDiagnosticLog.start(this);
        synchronized (LOCK) {
            running = true;
            localPlayerId = safe(nextLocalPlayerId);
            lastStatus = "foreground connectedDevice";
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=iff_radio_service_start"
                + " lifecycle=FOREGROUND_SERVICE_CONNECTED_DEVICE"
                + " localPlayerId=" + localPlayerId
                + " policy=\"" + clean(IffRadioWitnessStore.freshnessPolicyLabel()) + "\"");
        IffFieldRunSummary.reset(localPlayerId);
        startLocationUpdates();
        IffBleFieldRadio.startFromForegroundService(this, localPlayerId);
        IffWifiDirectDiscoveryTransport.start(this, localPlayerId);
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
        Location localGpsLocation = readBestLocation();
        IffBleFieldRadio.updateLocalGps(localGpsLocation);
        IffRemoteWitnessReport remoteGpsReport = IffRemoteWitnessStore.getFreshGpsReportFor(distanceTarget.playerId);
        IffGpsSnapshot gpsSnapshot = readGpsSnapshot(
                localGpsLocation,
                remoteGpsReport,
                distanceTarget.trend.distanceClass);
        WifiFingerprintSnapshot wifiSnapshot = readWifiFingerprintSnapshot(currentLocalPlayerId);
        IffFieldLocatorSnapshot locatorSnapshot = IffFieldLocatorSnapshot.from(
                IffWifiTargetObservationStore.snapshot(),
                distanceTarget.trend,
                gpsSnapshot);
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
                + " gpsLocalProvider=" + safe(localGpsLocation == null ? "" : localGpsLocation.getProvider())
                + " gpsLocalCluster=" + gpsCluster(localGpsLocation)
                + " gpsRemoteCluster=" + gpsCluster(remoteGpsReport)
                + " fieldRadioStatus=\"" + clean(IffBleFieldRadio.compactStatus()) + "\""
                + " fieldRadioPolicy=\"" + clean(IffBleFieldRadio.lifecycleStatus()) + "\""
                + " fieldLocatorStatus=\"" + clean(locatorSnapshot.compact()) + "\""
                + " wifiDirectStatus=\"" + clean(IffWifiDirectDiscoveryTransport.compactStatus()) + "\""
                + " wifiTargetStatus=\"" + clean(IffWifiTargetObservationStore.compactStatus()) + "\""
                + " wifiFingerprintStatus=" + safe(wifiSnapshot.status)
                + " wifiRefreshRequested=" + wifiSnapshot.refreshRequested
                + " wifiRefreshAccepted=" + wifiSnapshot.refreshAccepted
                + " wifiFreshness=" + safe(wifiSnapshot.freshness)
                + " wifiFreshAgeMs=" + wifiSnapshot.freshAgeMs
                + " wifiFingerprint=\"" + clean(wifiSnapshot.fingerprint) + "\""
                + " transportStatus=\"" + clean(IffUdpWitnessTransport.compactStatus()) + "\"");
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
        if (latest != null && Math.max(0L, System.currentTimeMillis() - latest.getTime()) <= ACCEPTED_LOCATION_FRESH_MS) {
            return latest;
        }
        if (best == null) {
            return latest;
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
        if (requestLocationProvider(locationManager, LocationManager.NETWORK_PROVIDER)) {
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

    private void stopLocationUpdates(String reason) {
        LocationManager locationManager = activeLocationManager;
        activeLocationManager = null;
        if (locationManager == null) {
            return;
        }
        try {
            locationManager.removeUpdates(locationListener);
            FieldDiagnosticLog.event("IFF_DIAG", "event=gps_updates_stop reason=" + safe(reason));
        } catch (Exception e) {
            FieldDiagnosticLog.event("IFF_DIAG", "event=gps_updates_stop reason=" + safe(reason)
                    + " error=\"" + clean(e.getClass().getSimpleName()) + "\"");
        }
    }

    private void rememberLocation(Location location, String source) {
        if (location == null) {
            return;
        }
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
                        + " cluster=" + gpsCluster(location));
                return;
            }
            latestLocation = new Location(location);
        }
        IffBleFieldRadio.updateLocalGps(location);
        long ageMs = Math.max(0L, System.currentTimeMillis() - location.getTime());
        FieldDiagnosticLog.event("IFF_DIAG", "event=gps_location_update"
                + " source=" + safe(source)
                + " provider=" + safe(location.getProvider())
                + " ageMs=" + ageMs
                + " accuracyM=" + (location.hasAccuracy() ? Math.round(location.getAccuracy()) : "na")
                + " bearingDeg=" + (location.hasBearing() ? Math.round(location.getBearing()) : "na"));
    }

    @Nullable
    private Location bestLastKnownLocation(LocationManager locationManager) {
        Location gps = lastKnown(locationManager, LocationManager.GPS_PROVIDER);
        Location network = lastKnown(locationManager, LocationManager.NETWORK_PROVIDER);
        if (gps == null) {
            return network;
        }
        if (network == null) {
            return gps;
        }
        if (gps.getTime() >= network.getTime()) {
            return gps;
        }
        return network;
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

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Compass IFF radio")
                .setContentText("BLE IFF foreground radio: " + safe(playerId))
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

    private static String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }
}
