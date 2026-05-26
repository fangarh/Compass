package net.afterday.compas.iff;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.Map;
import net.afterday.compas.logging.FieldDiagnosticLog;

@TargetApi(16)
public final class IffWifiDirectDiscoveryTransport {
    private static final Object LOCK = new Object();
    private static final long REFRESH_MS = 1000L;
    private static WifiP2pManager manager;
    private static WifiP2pManager.Channel channel;
    private static WifiP2pServiceRequest serviceRequest;
    private static Context appContext;
    private static Handler handler;
    private static boolean running;
    private static String localPlayerId = "";
    private static String targetPlayerId = "";
    private static int targetRssi;
    private static Location latestLocalGps;
    private static String lastStatus = "idle";
    private static long sequence;

    private static final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshLocalServiceAndDiscovery();
            Handler nextHandler;
            synchronized (LOCK) {
                nextHandler = handler;
            }
            if (nextHandler != null) {
                nextHandler.postDelayed(this, REFRESH_MS);
            }
        }
    };

    private IffWifiDirectDiscoveryTransport() {
    }

    public static void start(Context context, String playerId) {
        if (context == null || Build.VERSION.SDK_INT < 16) {
            setStatus("unsupported");
            return;
        }
        Context nextContext = context.getApplicationContext();
        if (!hasPermissions(nextContext)) {
            setStatus("missing_permission");
            FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_status state=missing_permission");
            return;
        }
        WifiP2pManager nextManager =
                (WifiP2pManager) nextContext.getSystemService(Context.WIFI_P2P_SERVICE);
        if (nextManager == null) {
            setStatus("manager_unavailable");
            FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_status state=manager_unavailable");
            return;
        }
        WifiP2pManager.Channel nextChannel = nextManager.initialize(
                nextContext,
                Looper.getMainLooper(),
                new WifiP2pManager.ChannelListener() {
                    @Override
                    public void onChannelDisconnected() {
                        synchronized (LOCK) {
                            running = false;
                            lastStatus = "channel_disconnected";
                        }
                        FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_status state=channel_disconnected");
                    }
                });
        if (nextChannel == null) {
            setStatus("channel_unavailable");
            FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_status state=channel_unavailable");
            return;
        }
        stop();
        synchronized (LOCK) {
            manager = nextManager;
            channel = nextChannel;
            appContext = nextContext;
            handler = new Handler(Looper.getMainLooper());
            running = true;
            localPlayerId = safe(playerId);
            lastStatus = "starting";
        }
        installListeners(nextManager, nextChannel);
        FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_start localPlayerId=" + safe(playerId)
                + " serviceType=" + IffWifiDirectPayload.SERVICE_TYPE);
        refreshLocalServiceAndDiscovery();
        synchronized (LOCK) {
            if (handler != null) {
                handler.removeCallbacks(refreshRunnable);
                handler.postDelayed(refreshRunnable, REFRESH_MS);
            }
        }
    }

    public static void stop() {
        WifiP2pManager currentManager;
        WifiP2pManager.Channel currentChannel;
        WifiP2pServiceRequest currentRequest;
        Handler currentHandler;
        synchronized (LOCK) {
            currentManager = manager;
            currentChannel = channel;
            currentRequest = serviceRequest;
            currentHandler = handler;
            manager = null;
            channel = null;
            serviceRequest = null;
            handler = null;
            appContext = null;
            running = false;
            lastStatus = "stopped";
        }
        if (currentHandler != null) {
            currentHandler.removeCallbacks(refreshRunnable);
        }
        if (currentManager != null && currentChannel != null) {
            try {
                if (currentRequest != null) {
                    currentManager.removeServiceRequest(currentChannel, currentRequest, null);
                }
                currentManager.clearLocalServices(currentChannel, null);
            } catch (Exception ignored) {
            }
        }
    }

    public static String compactStatus() {
        synchronized (LOCK) {
            return "wfd " + (running ? "on" : "off")
                    + " local=" + localPlayerId
                    + " seq=" + sequence
                    + " " + lastStatus;
        }
    }

    public static void updateTargetObservation(String nextTargetPlayerId, int nextTargetRssi) {
        synchronized (LOCK) {
            targetPlayerId = safe(nextTargetPlayerId);
            targetRssi = nextTargetRssi;
        }
    }

    public static void updateLocalGps(Location location) {
        synchronized (LOCK) {
            latestLocalGps = location == null ? null : new Location(location);
        }
    }

    private static void installListeners(WifiP2pManager nextManager, WifiP2pManager.Channel nextChannel) {
        nextManager.setDnsSdResponseListeners(
                nextChannel,
                new WifiP2pManager.DnsSdServiceResponseListener() {
                    @Override
                    public void onDnsSdServiceAvailable(
                            String instanceName,
                            String registrationType,
                            WifiP2pDevice srcDevice) {
                        IffWifiDirectPayload.Parsed parsed =
                                IffWifiDirectPayload.parseInstanceName(instanceName);
                        if (parsed != null) {
                            synchronized (LOCK) {
                                lastStatus = "service_rx " + parsed.playerId + " seq=" + parsed.sequence;
                            }
                            FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_payload_rx"
                                    + " channel=service_name"
                                    + " playerId=" + clean(parsed.playerId)
                                    + " sequence=" + parsed.sequence
                                    + " deviceName=\"" + clean(srcDevice == null ? "" : srcDevice.deviceName) + "\""
                                    + " address=" + clean(srcDevice == null ? "" : srcDevice.deviceAddress));
                            recordTargetObservation(parsed);
                        }
                        FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_service_rx"
                                + " instance=" + clean(instanceName)
                                + " type=" + clean(registrationType)
                                + " deviceName=\"" + clean(srcDevice == null ? "" : srcDevice.deviceName) + "\""
                                + " address=" + clean(srcDevice == null ? "" : srcDevice.deviceAddress));
                    }
                },
                new WifiP2pManager.DnsSdTxtRecordListener() {
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName,
                            Map<String, String> txtRecordMap,
                            WifiP2pDevice srcDevice) {
                        IffWifiDirectPayload.Parsed parsed = IffWifiDirectPayload.parse(txtRecordMap);
                        if (parsed == null) {
                            return;
                        }
                        synchronized (LOCK) {
                            lastStatus = "rx " + parsed.playerId + " seq=" + parsed.sequence;
                        }
                        FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_txt_rx"
                                + " playerId=" + clean(parsed.playerId)
                                + " sequence=" + parsed.sequence
                                + " ageMs=" + Math.max(0L, System.currentTimeMillis() - parsed.timestampMs)
                                + " domain=" + clean(fullDomainName)
                                + " deviceName=\"" + clean(srcDevice == null ? "" : srcDevice.deviceName) + "\""
                                + " address=" + clean(srcDevice == null ? "" : srcDevice.deviceAddress));
                        recordCoordinateMessage(parsed);
                        recordGpsReports(parsed, srcDevice);
                        recordTargetObservation(parsed);
                    }
                });
    }

    private static void refreshLocalServiceAndDiscovery() {
        WifiP2pManager currentManager;
        WifiP2pManager.Channel currentChannel;
        String playerId;
        String currentTargetPlayerId;
        int currentTargetRssi;
        Location localGps;
        long nextSequence;
        synchronized (LOCK) {
            if (!running || manager == null || channel == null) {
                return;
            }
            currentManager = manager;
            currentChannel = channel;
            playerId = localPlayerId;
            currentTargetPlayerId = targetPlayerId;
            currentTargetRssi = targetRssi;
            localGps = latestLocalGps == null ? null : new Location(latestLocalGps);
            sequence++;
            nextSequence = sequence;
        }
        IffRemoteWitnessReport targetGpsReport = currentTargetPlayerId.length() == 0
                ? null
                : IffRemoteWitnessStore.getFreshGpsReportFor(currentTargetPlayerId);
        Map<String, String> txt = txtPayload(
                playerId,
                nextSequence,
                System.currentTimeMillis(),
                localGps,
                currentTargetPlayerId,
                currentTargetRssi,
                targetGpsReport);
        final WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(
                IffWifiDirectPayload.buildInstanceName(
                        playerId,
                        nextSequence,
                        currentTargetPlayerId,
                        currentTargetRssi),
                IffWifiDirectPayload.SERVICE_TYPE,
                txt);
        try {
            currentManager.clearLocalServices(currentChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    addLocalService(serviceInfo);
                }

                @Override
                public void onFailure(int reason) {
                    setStatus("clear_services_failed_" + reason);
                    FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_local_service_clear ok=false reason=" + reason);
                    addLocalService(serviceInfo);
                }
            });
        } catch (Exception e) {
            setStatus("refresh_exception");
            FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_status state=refresh_exception error=\""
                    + clean(e.getClass().getSimpleName()) + "\"");
        }
    }

    private static void addLocalService(WifiP2pDnsSdServiceInfo serviceInfo) {
        WifiP2pManager currentManager;
        WifiP2pManager.Channel currentChannel;
        synchronized (LOCK) {
            currentManager = manager;
            currentChannel = channel;
        }
        if (currentManager == null || currentChannel == null) {
            return;
        }
        currentManager.addLocalService(currentChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                setStatus("service_published");
                FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_local_service ok=true");
                restartDiscovery();
            }

            @Override
            public void onFailure(int reason) {
                setStatus("service_publish_failed_" + reason);
                FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_local_service ok=false reason=" + reason);
                restartDiscovery();
            }
        });
    }

    private static void restartDiscovery() {
        WifiP2pManager currentManager;
        WifiP2pManager.Channel currentChannel;
        WifiP2pServiceRequest previousRequest;
        WifiP2pServiceRequest nextRequest;
        synchronized (LOCK) {
            currentManager = manager;
            currentChannel = channel;
            previousRequest = serviceRequest;
            nextRequest = newServiceRequest();
            serviceRequest = nextRequest;
        }
        if (currentManager == null || currentChannel == null) {
            return;
        }
        if (nextRequest == null) {
            setStatus("request_unavailable");
            FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_service_request ok=false reason=null_request");
        }
        try {
            if (previousRequest != null) {
                currentManager.removeServiceRequest(currentChannel, previousRequest, null);
            }
            currentManager.discoverPeers(currentChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    setStatus("peer_discovery");
                    FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_discover_peers ok=true");
                    requestPeersSoon();
                    addServiceRequestAndDiscover();
                }

                @Override
                public void onFailure(int reason) {
                    setStatus("peer_discovery_failed_" + reason);
                    FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_discover_peers ok=false reason=" + reason);
                    addServiceRequestAndDiscover();
                }
            });
        } catch (Exception e) {
            setStatus("peer_discovery_exception");
            FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_discover_peers ok=false error=\""
                    + clean(e.getClass().getSimpleName()) + "\"");
        }
    }

    private static void addServiceRequestAndDiscover() {
        WifiP2pManager currentManager;
        WifiP2pManager.Channel currentChannel;
        WifiP2pServiceRequest request;
        synchronized (LOCK) {
            currentManager = manager;
            currentChannel = channel;
            request = serviceRequest;
        }
        if (currentManager == null || currentChannel == null) {
            return;
        }
        if (request == null) {
            request = ensureServiceRequest();
        }
        if (request == null) {
            setStatus("request_unavailable");
            FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_service_request ok=false reason=null_request");
            return;
        }
        try {
            currentManager.addServiceRequest(currentChannel, request, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    discoverServices();
                }

                @Override
                public void onFailure(int reason) {
                    setStatus("request_failed_" + reason);
                    FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_service_request ok=false reason=" + reason);
                }
            });
        } catch (Exception e) {
            setStatus("request_exception");
            FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_service_request ok=false error=\""
                    + clean(e.getClass().getSimpleName()) + "\"");
        }
    }

    private static WifiP2pServiceRequest ensureServiceRequest() {
        WifiP2pServiceRequest currentRequest;
        synchronized (LOCK) {
            currentRequest = serviceRequest;
        }
        if (currentRequest != null) {
            return currentRequest;
        }
        WifiP2pServiceRequest nextRequest = newServiceRequest();
        synchronized (LOCK) {
            if (serviceRequest == null) {
                serviceRequest = nextRequest;
            }
            return serviceRequest;
        }
    }

    private static WifiP2pServiceRequest newServiceRequest() {
        try {
            WifiP2pServiceRequest typedRequest =
                    WifiP2pDnsSdServiceRequest.newInstance(IffWifiDirectPayload.SERVICE_TYPE);
            if (typedRequest != null) {
                return typedRequest;
            }
        } catch (Exception e) {
            FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_service_request_create ok=false mode=typed error=\""
                    + clean(e.getClass().getSimpleName()) + "\"");
        }
        try {
            return WifiP2pDnsSdServiceRequest.newInstance();
        } catch (Exception e) {
            FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_service_request_create ok=false mode=all error=\""
                    + clean(e.getClass().getSimpleName()) + "\"");
            return null;
        }
    }

    private static void requestPeersSoon() {
        Handler currentHandler;
        synchronized (LOCK) {
            currentHandler = handler;
        }
        if (currentHandler == null) {
            return;
        }
        currentHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                requestPeers();
            }
        }, 3000L);
    }

    private static void requestPeers() {
        WifiP2pManager currentManager;
        WifiP2pManager.Channel currentChannel;
        synchronized (LOCK) {
            currentManager = manager;
            currentChannel = channel;
        }
        if (currentManager == null || currentChannel == null) {
            return;
        }
        try {
            currentManager.requestPeers(currentChannel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peers) {
                    int count = peers == null ? 0 : peers.getDeviceList().size();
                    FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_peers count=" + count);
                    if (peers == null) {
                        return;
                    }
                    for (WifiP2pDevice device : peers.getDeviceList()) {
                        FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_peer"
                                + " deviceName=\"" + clean(device.deviceName) + "\""
                                + " address=" + clean(device.deviceAddress)
                                + " status=" + device.status);
                    }
                }
            });
        } catch (Exception e) {
            FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_peers error=\""
                    + clean(e.getClass().getSimpleName()) + "\"");
        }
    }

    private static void discoverServices() {
        WifiP2pManager currentManager;
        WifiP2pManager.Channel currentChannel;
        synchronized (LOCK) {
            currentManager = manager;
            currentChannel = channel;
        }
        if (currentManager == null || currentChannel == null) {
            return;
        }
        currentManager.discoverServices(currentChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                setStatus("discovering");
                FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_discover_services ok=true");
            }

            @Override
            public void onFailure(int reason) {
                setStatus("discover_failed_" + reason);
                FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_discover_services ok=false reason=" + reason);
            }
        });
    }

    private static boolean hasPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 23
                && context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    private static void setStatus(String status) {
        synchronized (LOCK) {
            lastStatus = safe(status);
        }
    }

    private static void recordTargetObservation(IffWifiDirectPayload.Parsed parsed) {
        if (parsed == null || parsed.targetPlayerId.length() == 0) {
            return;
        }
        IffWifiTargetObservationStore.updateRemoteObservation(
                parsed.playerId,
                parsed.targetPlayerId,
                parsed.targetRssi);
        FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_target_observation_rx"
                + " anchorPlayerId=" + clean(parsed.playerId)
                + " targetPlayerId=" + clean(parsed.targetPlayerId)
                + " rssi=" + parsed.targetRssi
                + " sequence=" + parsed.sequence);
    }

    private static void recordCoordinateMessage(IffWifiDirectPayload.Parsed parsed) {
        if (parsed == null || parsed.coordinateMessage.length() == 0) {
            return;
        }
        IffCoordinateMessage.Parsed coordinates =
                IffCoordinateMessage.parse(parsed.coordinateMessage, SystemClock.elapsedRealtime());
        if (coordinates == null) {
            FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_coordinates_rx"
                    + " accepted=false"
                    + " playerId=" + clean(parsed.playerId)
                    + " sequence=" + parsed.sequence
                    + " stateCount=0");
            return;
        }
        for (int i = 0; i < coordinates.states.size(); i++) {
            IffForegroundRadioService.mergeParticipantState(coordinates.states.get(i));
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=wifi_direct_coordinates_rx"
                + " accepted=true"
                + " senderPlayerId=" + clean(coordinates.senderPlayerId)
                + " payloadPlayerId=" + clean(parsed.playerId)
                + " sequence=" + coordinates.sequence
                + " stateCount=" + coordinates.states.size());
    }

    private static void recordGpsReports(IffWifiDirectPayload.Parsed parsed, WifiP2pDevice srcDevice) {
        if (parsed == null) {
            return;
        }
        String address = srcDevice == null ? "" : srcDevice.deviceAddress;
        if (parsed.hasGps) {
            receiveGpsReport(
                    "wfd-" + parsed.playerId,
                    parsed.playerId,
                    -1,
                    parsed.gpsLatE7,
                    parsed.gpsLonE7,
                    parsed.gpsAccuracyM,
                    parsed.gpsAgeMs,
                    address,
                    "wifi_direct_gps_rx");
        }
        if (parsed.hasTargetGps && parsed.targetPlayerId.length() > 0) {
            receiveGpsReport(
                    "wfd-relay-" + parsed.playerId,
                    parsed.targetPlayerId,
                    parsed.targetRssi,
                    parsed.targetGpsLatE7,
                    parsed.targetGpsLonE7,
                    parsed.targetGpsAccuracyM,
                    parsed.targetGpsAgeMs,
                    address,
                    "wifi_direct_target_gps_rx");
        }
    }

    private static void receiveGpsReport(
            String sourcePlayerId,
            String targetPlayerId,
            int rssi,
            int gpsLatE7,
            int gpsLonE7,
            int gpsAccuracyM,
            long gpsAgeMs,
            String address,
            String event) {
        long now = SystemClock.elapsedRealtime();
        long observedElapsedMs = now - Math.max(0L, gpsAgeMs);
        boolean accepted = IffRemoteWitnessStore.receiveReport(new IffRemoteWitnessReport(
                sourcePlayerId,
                targetPlayerId,
                IffRadioWitnessStore.expectedBeaconSsid(targetPlayerId),
                "wfd:" + safe(address),
                rssi,
                0,
                observedElapsedMs,
                now,
                IffRemoteWitnessReport.SIGNATURE_PENDING,
                gpsLatE7,
                gpsLonE7,
                gpsAccuracyM,
                observedElapsedMs));
        FieldDiagnosticLog.event("IFF_DIAG", "event=" + event
                + " accepted=" + accepted
                + " sourcePlayerId=" + clean(sourcePlayerId)
                + " targetPlayerId=" + clean(targetPlayerId)
                + " gpsAgeMs=" + gpsAgeMs
                + " gpsAccuracyM=" + gpsAccuracyM);
    }

    private static Map<String, String> txtPayload(
            String playerId,
            long sequence,
            long timestampMs,
            Location localGps,
            String currentTargetPlayerId,
            int currentTargetRssi,
            IffRemoteWitnessReport targetGpsReport) {
        boolean hasLocalGps = hasFreshGps(localGps);
        boolean hasTargetGps = targetGpsReport != null && targetGpsReport.hasGpsFix();
        Map<String, String> txt;
        if (hasLocalGps && hasTargetGps) {
            txt = IffWifiDirectPayload.build(
                    playerId,
                    sequence,
                    timestampMs,
                    IffRemoteWitnessFrame.coordinateE7(localGps.getLatitude()),
                    IffRemoteWitnessFrame.coordinateE7(localGps.getLongitude()),
                    localGps.hasAccuracy() ? Math.round(localGps.getAccuracy()) : 0,
                    Math.max(0L, System.currentTimeMillis() - localGps.getTime()),
                    currentTargetPlayerId,
                    currentTargetRssi,
                    targetGpsReport.gpsLatE7,
                    targetGpsReport.gpsLonE7,
                    targetGpsReport.gpsAccuracyM,
                    targetGpsReport.gpsAgeMs());
        } else if (hasLocalGps) {
            txt = IffWifiDirectPayload.build(
                    playerId,
                    sequence,
                    timestampMs,
                    IffRemoteWitnessFrame.coordinateE7(localGps.getLatitude()),
                    IffRemoteWitnessFrame.coordinateE7(localGps.getLongitude()),
                    localGps.hasAccuracy() ? Math.round(localGps.getAccuracy()) : 0,
                    Math.max(0L, System.currentTimeMillis() - localGps.getTime()));
        } else if (hasTargetGps) {
            txt = IffWifiDirectPayload.build(
                    playerId,
                    sequence,
                    timestampMs,
                    currentTargetPlayerId,
                    currentTargetRssi,
                    targetGpsReport.gpsLatE7,
                    targetGpsReport.gpsLonE7,
                    targetGpsReport.gpsAccuracyM,
                    targetGpsReport.gpsAgeMs());
        } else {
            txt = IffWifiDirectPayload.build(playerId, sequence, timestampMs);
        }
        IffWifiDirectPayload.putCoordinateMessage(
                txt,
                IffForegroundRadioService.coordinateMessageForBroadcast(playerId, sequence));
        return txt;
    }

    private static boolean hasFreshGps(Location location) {
        return location != null
                && IffGpsSanity.isPlausibleCoordinate(location.getLatitude(), location.getLongitude())
                && System.currentTimeMillis() - location.getTime() <= 15000L;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }
}
