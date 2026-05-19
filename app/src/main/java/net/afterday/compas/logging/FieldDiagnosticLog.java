package net.afterday.compas.logging;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        logDeviceContext(context.getApplicationContext());
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

    private static void logDeviceContext(Context context) {
        event("FIELD_DIAG", "event=device_context"
                + " manufacturer=\"" + safe(Build.MANUFACTURER) + "\""
                + " brand=\"" + safe(Build.BRAND) + "\""
                + " model=\"" + safe(Build.MODEL) + "\""
                + " device=\"" + safe(Build.DEVICE) + "\""
                + " product=\"" + safe(Build.PRODUCT) + "\""
                + " hardware=\"" + safe(Build.HARDWARE) + "\""
                + " sdk=" + Build.VERSION.SDK_INT
                + " release=\"" + safe(Build.VERSION.RELEASE) + "\""
                + " appVersionName=\"" + safe(getVersionName(context)) + "\""
                + " appVersionCode=" + getVersionCode(context)
                + " androidIdHash=\"" + safe(getAndroidIdHash(context)) + "\""
                + " batteryPercent=" + getBatteryPercent(context)
                + " charging=" + isCharging(context)
                + " powerSave=" + isPowerSaveMode(context)
                + " wifiEnabled=" + isWifiEnabled(context)
                + " locationEnabled=" + isLocationEnabled(context));
    }

    private static String getVersionName(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private static long getVersionCode(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= 28) {
                return info.getLongVersionCode();
            }
            return info.versionCode;
        } catch (Exception e) {
            return -1;
        }
    }

    private static String getAndroidIdHash(Context context) {
        try {
            String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId == null) {
                return "";
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(androidId.getBytes("UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < hash.length && i < 8; i++) {
                builder.append(String.format(Locale.US, "%02x", Integer.valueOf(hash[i] & 255)));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        } catch (Exception e2) {
            return "";
        }
    }

    private static int getBatteryPercent(Context context) {
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) {
            return -1;
        }
        int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level < 0 || scale <= 0) {
            return -1;
        }
        return Math.round((level * 100.0f) / scale);
    }

    private static boolean isCharging(Context context) {
        Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (battery == null) {
            return false;
        }
        int status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
    }

    private static boolean isPowerSaveMode(Context context) {
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager != null && powerManager.isPowerSaveMode();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isWifiEnabled(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return wifiManager != null && wifiManager.isWifiEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isLocationEnabled(Context context) {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                return false;
            }
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ');
    }
}
