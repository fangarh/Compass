package net.afterday.compas.iff;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import net.afterday.compas.IffActivity;
import net.afterday.compas.R;
import net.afterday.compas.logging.FieldDiagnosticLog;

public final class IffForegroundRadioService extends Service {
    private static final String ACTION_START = "net.afterday.compas.iff.START_RADIO";
    private static final String ACTION_STOP = "net.afterday.compas.iff.STOP_RADIO";
    private static final String EXTRA_LOCAL_PLAYER_ID = "localPlayerId";
    private static final String CHANNEL_ID = "compass_iff_radio";
    private static final int NOTIFICATION_ID = 2701;
    private static final Object LOCK = new Object();

    private static boolean running;
    private static String localPlayerId = "";
    private static String lastStatus = "service idle";

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
        synchronized (LOCK) {
            running = true;
            localPlayerId = safe(nextLocalPlayerId);
            lastStatus = "foreground connectedDevice";
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=iff_radio_service_start"
                + " lifecycle=FOREGROUND_SERVICE_CONNECTED_DEVICE"
                + " localPlayerId=" + localPlayerId
                + " policy=\"" + clean(IffRadioWitnessStore.freshnessPolicyLabel()) + "\"");
        IffBleFieldRadio.startFromForegroundService(this, localPlayerId);
    }

    private void stopRadio(String reason) {
        IffBleFieldRadio.stop(reason);
        synchronized (LOCK) {
            running = false;
            lastStatus = "stopped " + safe(reason);
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=iff_radio_service_stop"
                + " reason=" + safe(reason)
                + " lifecycle=FOREGROUND_SERVICE_CONNECTED_DEVICE");
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
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
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
