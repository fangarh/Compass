package net.afterday.compas.iff;

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
    private static final String FRAME_PREFIX = "COMPASS_IFF_REMOTE";
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
        ensureStarted();
        final String sourcePlayerId = sourcePlayerId();
        final long ageMs = witness == null ? 2500L : witness.ageMs();
        final int rssi = witness == null ? -63 : witness.rssi;
        final int frequency = witness == null ? 2412 : witness.frequency;
        final String bssid = witness == null ? "udp:stub:" + sourcePlayerId + ":" + targetPlayerId : witness.bssid;
        final String mode = witness == null ? "stub_no_local_witness" : "local_witness";
        final String target = targetPlayerId;
        String frame = FRAME_PREFIX
                + "|v=" + IffRemoteWitnessReport.CONTRACT_VERSION
                + "|source=" + cleanToken(sourcePlayerId)
                + "|target=" + cleanToken(target)
                + "|ssid=" + cleanToken(targetBeaconSsid)
                + "|bssid=" + cleanToken(bssid)
                + "|rssi=" + rssi
                + "|freq=" + frequency
                + "|ageMs=" + Math.max(0L, ageMs)
                + "|signature=" + IffRemoteWitnessReport.SIGNATURE_PENDING;

        final String frameToSend = frame;
        synchronized (LOCK) {
            lastStatus = "tx queued " + target;
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=remote_witness_udp_tx_queued"
                + " sourcePlayerId=" + sourcePlayerId
                + " targetPlayerId=" + target
                + " mode=" + mode
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
        if (frame == null || !frame.startsWith(FRAME_PREFIX + "|")) {
            return null;
        }
        Map<String, String> fields = new HashMap<>();
        String[] parts = frame.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            int split = parts[i].indexOf('=');
            if (split <= 0 || split >= parts[i].length() - 1) {
                continue;
            }
            fields.put(parts[i].substring(0, split), parts[i].substring(split + 1));
        }
        if (!IffRemoteWitnessReport.CONTRACT_VERSION.equals(fields.get("v"))) {
            return null;
        }
        long ageMs = parseLong(fields.get("ageMs"), -1L);
        int rssi = parseInt(fields.get("rssi"), 0);
        int frequency = parseInt(fields.get("freq"), 0);
        if (ageMs < 0L || rssi == 0 || frequency == 0) {
            return null;
        }
        long now = SystemClock.elapsedRealtime();
        return new IffRemoteWitnessReport(
                fields.get("source"),
                fields.get("target"),
                fields.get("ssid"),
                fields.get("bssid"),
                rssi,
                frequency,
                now - ageMs,
                now,
                fields.get("signature"));
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

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return fallback;
        }
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
}
