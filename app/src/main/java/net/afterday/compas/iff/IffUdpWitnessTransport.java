package net.afterday.compas.iff;

import android.location.Location;
import android.os.Build;
import android.os.SystemClock;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.afterday.compas.iff.IffRadioWitnessStore.WitnessSnapshot;
import net.afterday.compas.logging.FieldDiagnosticLog;

public final class IffUdpWitnessTransport {
    private static final Object LOCK = new Object();
    private static final int PORT = 45873;
    private static final int MAX_PACKET_BYTES = 2048;

    private static DatagramSocket receiverSocket;
    private static Thread receiverThread;
    private static boolean running;
    private static int txCount;
    private static int rxCount;
    private static int rejectedCount;
    private static String lastStatus = "idle";

    private IffUdpWitnessTransport() {
    }

    public static void ensureStarted() {
        synchronized (LOCK) {
            if (running) {
                return;
            }
            running = true;
            receiverThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    receiveLoop();
                }
            }, "iff-udp-witness");
            receiverThread.setDaemon(true);
            receiverThread.start();
        }
    }

    public static void stop() {
        synchronized (LOCK) {
            running = false;
            if (receiverSocket != null) {
                receiverSocket.close();
                receiverSocket = null;
            }
        }
    }

    public static boolean sendReport(String targetPlayerId, String targetBeaconSsid, WitnessSnapshot witness) {
        return sendReport(targetPlayerId, targetBeaconSsid, witness, null);
    }

    public static boolean sendReport(String targetPlayerId, String targetBeaconSsid, WitnessSnapshot witness,
                                     Location gpsLocation) {
        ensureStarted();
        final String sourcePlayerId = sourcePlayerId();
        final long ageMs = witness == null ? 2500L : witness.ageMs();
        final int rssi = witness == null ? -63 : witness.rssi;
        final int frequency = witness == null ? 2412 : witness.frequency;
        final String bssid = witness == null ? "udp:stub:" + sourcePlayerId + ":" + targetPlayerId : witness.bssid;
        final String mode = witness == null ? "stub_no_local_witness" : "local_witness";
        final String target = targetPlayerId;
        IffRemoteWitnessFrame frame = new IffRemoteWitnessFrame(
                IffRemoteWitnessReport.CONTRACT_VERSION,
                sourcePlayerId,
                target,
                targetBeaconSsid,
                bssid,
                rssi,
                frequency,
                ageMs,
                IffRemoteWitnessReport.SIGNATURE_PENDING,
                gpsLatE7(gpsLocation),
                gpsLonE7(gpsLocation),
                gpsAccuracyM(gpsLocation),
                gpsAgeMs(gpsLocation));

        final String frameToSend = frame.toWire();
        synchronized (LOCK) {
            lastStatus = "tx queued " + target;
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=remote_witness_udp_tx_queued"
                + " sourcePlayerId=" + sourcePlayerId
                + " targetPlayerId=" + target
                + " mode=" + mode
                + " gps=" + frame.hasGps()
                + " contract=" + IffRemoteWitnessReport.CONTRACT_VERSION
                + " signatureStatus=" + IffRemoteWitnessReport.SIGNATURE_PENDING);
        Thread sender = new Thread(new Runnable() {
            @Override
            public void run() {
                sendFrameToBroadcasts(frameToSend, sourcePlayerId, target, mode);
            }
        }, "iff-udp-witness-tx");
        sender.setDaemon(true);
        sender.start();
        return true;
    }

    private static void sendFrameToBroadcasts(String frame, String sourcePlayerId, String targetPlayerId, String mode) {
        boolean sent = false;
        List<InetAddress> addresses = broadcastAddresses();
        for (int i = 0; i < addresses.size(); i++) {
            sent = sendFrame(frame, addresses.get(i)) || sent;
        }
        synchronized (LOCK) {
            if (sent) {
                txCount++;
                lastStatus = "tx " + targetPlayerId + " " + mode;
            } else {
                rejectedCount++;
                lastStatus = "tx failed " + targetPlayerId;
            }
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=remote_witness_udp_tx"
                + " sent=" + sent
                + " sourcePlayerId=" + sourcePlayerId
                + " targetPlayerId=" + targetPlayerId
                + " mode=" + mode
                + " broadcastCount=" + addresses.size()
                + " contract=" + IffRemoteWitnessReport.CONTRACT_VERSION
                + " signatureStatus=" + IffRemoteWitnessReport.SIGNATURE_PENDING);
    }

    public static String compactStatus() {
        synchronized (LOCK) {
            return "udp:" + PORT + " tx=" + txCount + " rx=" + rxCount + " rejected=" + rejectedCount
                    + " " + lastStatus;
        }
    }

    public static String sourcePlayerId() {
        return "debug-" + cleanToken(Build.MODEL).toLowerCase();
    }

    private static void receiveLoop() {
        try {
            DatagramSocket socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.setBroadcast(true);
            socket.bind(new InetSocketAddress(PORT));
            synchronized (LOCK) {
                receiverSocket = socket;
                lastStatus = "listening";
            }
            FieldDiagnosticLog.event("IFF_DIAG", "event=remote_witness_udp_listen"
                    + " port=" + PORT
                    + " sourcePlayerId=" + sourcePlayerId());
            byte[] buffer = new byte[MAX_PACKET_BYTES];
            while (isRunning()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String frame = new String(packet.getData(), packet.getOffset(), packet.getLength(), "UTF-8");
                handleFrame(frame, packet.getAddress());
            }
        } catch (Exception e) {
            if (isRunning()) {
                synchronized (LOCK) {
                    rejectedCount++;
                    lastStatus = "rx error";
                }
                FieldDiagnosticLog.event("IFF_DIAG", "event=remote_witness_udp_error"
                        + " error=\"" + cleanText(e.getClass().getSimpleName()) + "\"");
            }
        } finally {
            synchronized (LOCK) {
                running = false;
                if (receiverSocket != null) {
                    receiverSocket.close();
                    receiverSocket = null;
                }
            }
        }
    }

    private static boolean isRunning() {
        synchronized (LOCK) {
            return running;
        }
    }

    private static void handleFrame(String frame, InetAddress address) {
        IffRemoteWitnessReport report = parseFrame(frame);
        if (report == null) {
            synchronized (LOCK) {
                rejectedCount++;
                lastStatus = "rx rejected";
            }
            FieldDiagnosticLog.event("IFF_DIAG", "event=remote_witness_udp_rejected"
                    + " reason=invalid_frame"
                    + " from=" + cleanToken(address == null ? "" : address.getHostAddress()));
            return;
        }
        if (sourcePlayerId().equals(report.sourcePlayerId)) {
            synchronized (LOCK) {
                lastStatus = "rx self ignored";
            }
            return;
        }
        boolean accepted = IffRemoteWitnessStore.receiveReport(report);
        synchronized (LOCK) {
            if (accepted) {
                rxCount++;
                lastStatus = "rx " + report.targetPlayerId;
            } else {
                rejectedCount++;
                lastStatus = "rx rejected";
            }
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=remote_witness_udp_rx"
                + " accepted=" + accepted
                + " from=" + cleanToken(address == null ? "" : address.getHostAddress())
                + " sourcePlayerId=" + report.sourcePlayerId
                + " targetPlayerId=" + report.targetPlayerId
                + " freshness=" + report.freshnessLabel()
                + " contract=" + IffRemoteWitnessReport.CONTRACT_VERSION);
    }

    private static IffRemoteWitnessReport parseFrame(String frame) {
        IffRemoteWitnessFrame parsed = IffRemoteWitnessFrame.parse(frame);
        if (parsed == null) {
            return null;
        }
        long now = SystemClock.elapsedRealtime();
        return new IffRemoteWitnessReport(
                parsed.sourcePlayerId,
                parsed.targetPlayerId,
                parsed.targetBeaconSsid,
                parsed.bssid,
                parsed.rssi,
                parsed.frequency,
                now - parsed.ageMs,
                now,
                parsed.signatureStatus,
                parsed.gpsLatE7,
                parsed.gpsLonE7,
                parsed.gpsAccuracyM,
                parsed.hasGps() ? now - parsed.gpsAgeMs : -1L);
    }

    private static boolean sendFrame(String frame, InetAddress address) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
            byte[] bytes = frame.getBytes("UTF-8");
            socket.send(new DatagramPacket(bytes, bytes.length, address, PORT));
            return true;
        } catch (Exception e) {
            FieldDiagnosticLog.event("IFF_DIAG", "event=remote_witness_udp_send_error"
                    + " target=" + cleanToken(address == null ? "" : address.getHostAddress())
                    + " error=\"" + cleanText(e.getClass().getSimpleName()) + "\"");
            return false;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private static List<InetAddress> broadcastAddresses() {
        List<InetAddress> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                for (int i = 0; i < interfaceAddresses.size(); i++) {
                    InetAddress broadcast = interfaceAddresses.get(i).getBroadcast();
                    if (broadcast != null && !addresses.contains(broadcast)) {
                        addresses.add(broadcast);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (addresses.size() == 0) {
            try {
                addresses.add(InetAddress.getByName("255.255.255.255"));
            } catch (Exception ignored) {
            }
        }
        return addresses;
    }

    private static String cleanToken(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('|', '_').replace('=', '_').replace('\n', ' ').replace('\r', ' ').replace(' ', '-');
    }

    private static String cleanText(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }

    private static int gpsLatE7(Location location) {
        return location == null ? IffRemoteWitnessFrame.GPS_UNAVAILABLE_INT
                : IffRemoteWitnessFrame.coordinateE7(location.getLatitude());
    }

    private static int gpsLonE7(Location location) {
        return location == null ? IffRemoteWitnessFrame.GPS_UNAVAILABLE_INT
                : IffRemoteWitnessFrame.coordinateE7(location.getLongitude());
    }

    private static int gpsAccuracyM(Location location) {
        if (location == null || !location.hasAccuracy()) {
            return IffRemoteWitnessFrame.GPS_UNAVAILABLE_INT;
        }
        return Math.round(location.getAccuracy());
    }

    private static long gpsAgeMs(Location location) {
        if (location == null) {
            return IffRemoteWitnessFrame.GPS_UNAVAILABLE_AGE_MS;
        }
        return Math.max(0L, System.currentTimeMillis() - location.getTime());
    }
}
