package net.afterday.compas.logging;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import net.afterday.compas.BuildConfig;

/* JADX INFO: loaded from: classes.dex */
public final class FieldDiagnosticLog {
    private static final String TAG = "FieldDiagnosticLog";
    private static final String DIR_NAME = "diagnostics";
    private static final Object LOCK = new Object();
    private static final SimpleDateFormat FILE_DATE = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
    private static final SimpleDateFormat LINE_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private static File logFile;
    private static boolean initAttempted = false;

    private FieldDiagnosticLog() {
    }

    public static void start(Context context) {
        if (context == null) {
            return;
        }
        synchronized (LOCK) {
            if (logFile != null || initAttempted) {
                return;
            }
            initAttempted = true;
            File dir = context.getExternalFilesDir(DIR_NAME);
            if (dir == null) {
                dir = new File(context.getFilesDir(), DIR_NAME);
            }
            if (!dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "Cannot create diagnostics directory: " + dir.getAbsolutePath());
                return;
            }
            logFile = new File(dir, "field-radio-" + FILE_DATE.format(new Date()) + ".log");
        }
        event("FIELD_DIAG", "event=logger_start path=" + logFile.getAbsolutePath() + " package=" + BuildConfig.APPLICATION_ID + " sdk=" + Build.VERSION.SDK_INT + " debug=" + BuildConfig.DEBUG);
    }

    public static void wifi(String message) {
        event("WIFI_DIAG", message);
    }

    public static void wifiScanResults(String source, boolean updated, List<ScanResult> results) {
        if (results == null) {
            wifi("event=scan_results source=" + source + " count=0 null=true");
            return;
        }
        wifi("event=scan_results source=" + source + " updated=" + updated + " count=" + results.size());
        for (int i = 0; i < results.size(); i++) {
            ScanResult result = results.get(i);
            if (result == null) {
                wifi("event=scan_entry index=" + i + " null=true");
                continue;
            }
            wifi("event=scan_entry index=" + i + " ssid=\"" + safe(result.SSID) + "\" bssid=" + safe(result.BSSID) + " level=" + result.level + " frequency=" + result.frequency + " timestamp=" + result.timestamp + " capabilities=\"" + safe(result.capabilities) + "\"");
        }
    }

    public static void event(String tag, String message) {
        File file;
        synchronized (LOCK) {
            file = logFile;
        }
        if (file == null) {
            return;
        }
        String line = LINE_DATE.format(new Date()) + " elapsedMs=" + SystemClock.elapsedRealtime() + " " + tag + " " + safe(message) + "\n";
        synchronized (LOCK) {
            try (FileWriter writer = new FileWriter(file, true)) {
                writer.write(line);
                writer.flush();
            } catch (IOException e) {
                Log.w(TAG, "Cannot write field diagnostic log", e);
            }
        }
    }

    public static File getLogFile() {
        synchronized (LOCK) {
            return logFile;
        }
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ');
    }
}
