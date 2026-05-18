package net.afterday.compas.sensors.WiFi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.afterday.compas.BuildConfig;
import net.afterday.compas.logging.Logger;

/* JADX INFO: loaded from: classes.dex */
public class WifiImpl implements WiFi {
    private static final long DEBUG_BYPASS_SCAN_INTERVAL_MS = 1000;
    private static final long THROTTLED_SCAN_INTERVAL_MS = 35000;
    private static final long TICK_INTERVAL_SECONDS = 1;
    private static final long THROTTLE_LOG_INTERVAL_MS = 30000;
    private static final long DIAGNOSTIC_LOG_INTERVAL_MS = 30000;
    private final Context appContext;
    private final WifiManager mWifi;
    private final BroadcastReceiver scanReceiver;
    private final String TAG = "WiFi sensor";
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private Subject<List<ScanResult>> wifiScans = PublishSubject.create();
    private Subject<Boolean> isRunningSubj = BehaviorSubject.createDefault(false);
    private boolean scanReceiverRegistered = false;
    private long lastScanRequestMs = 0;
    private long lastThrottleLogMs = 0;
    private long lastFreshResultsMs = 0;
    private long lastDiagnosticLogMs = 0;
    private boolean lastDebugBypass = false;

    public WifiImpl(Context context) {
        this.appContext = context.getApplicationContext();
        this.mWifi = (WifiManager) this.appContext.getSystemService(Context.WIFI_SERVICE);
        this.scanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WifiImpl.this.onScanResultsAvailable(intent);
            }
        };
        this.isRunningSubj.switchMap($$Lambda$WifiImpl$liPl45VlIEThO0QWZJdmUzmiY8Y.INSTANCE).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$WifiImpl$2L5vfrJWdmFqDMkLzupp_4mVJw(this));
    }

    static /* synthetic */ ObservableSource lambda$new$0(Boolean isRunning) {
        return isRunning.booleanValue() ? Observable.interval(0L, TICK_INTERVAL_SECONDS, TimeUnit.SECONDS) : Observable.empty();
    }

    public /* synthetic */ void lambda$new$1$WifiImpl(Long t) {
        requestScanIfDue(false);
        publishCachedResults();
        logPeriodicDiagnostic();
    }

    @Override // net.afterday.compas.sensors.Sensor
    public void start() {
        Log.d(this.TAG, "WIFI Sensor started " + Thread.currentThread().getName());
        registerScanReceiver();
        this.isRunning.set(true);
        this.lastDebugBypass = isDebugBypassEnabled();
        logDiagnostic("WiFi scan mode=" + getModeName() + "; intervalMs=" + getScanIntervalMs() + "; debugBuild=" + BuildConfig.DEBUG, true);
        this.isRunningSubj.onNext(true);
        requestScanIfDue(true);
        publishCachedResults();
    }

    @Override // net.afterday.compas.sensors.Sensor
    public void stop() {
        this.isRunning.set(false);
        this.isRunningSubj.onNext(false);
        unregisterScanReceiver();
    }

    @Override // net.afterday.compas.sensors.Sensor
    public Observable<List<ScanResult>> getSensorResultsStream() {
        return this.wifiScans;
    }

    private void registerScanReceiver() {
        if (this.scanReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if (Build.VERSION.SDK_INT >= 33) {
            this.appContext.registerReceiver(this.scanReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            this.appContext.registerReceiver(this.scanReceiver, filter);
        }
        this.scanReceiverRegistered = true;
    }

    private void unregisterScanReceiver() {
        if (!this.scanReceiverRegistered) {
            return;
        }
        try {
            this.appContext.unregisterReceiver(this.scanReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(this.TAG, "WiFi scan receiver was already unregistered", e);
        }
        this.scanReceiverRegistered = false;
    }

    private void onScanResultsAvailable(Intent intent) {
        if (intent == null || !WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
            return;
        }
        boolean resultsUpdated = intent.getBooleanExtra("resultsUpdated", false);
        List<ScanResult> results = getScanResultsSafely();
        if (results == null) {
            return;
        }
        if (resultsUpdated) {
            this.lastFreshResultsMs = SystemClock.elapsedRealtime();
        }
        Log.d(this.TAG, "WIFI_DIAG event=results source=receiver updated=" + resultsUpdated + " count=" + results.size() + " mode=" + getModeName());
        publishResults(results);
    }

    private void requestScanIfDue(boolean force) {
        if (this.mWifi == null || !this.isRunning.get()) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        long intervalMs = getScanIntervalMs();
        if (!force && now - this.lastScanRequestMs < intervalMs) {
            return;
        }
        this.lastScanRequestMs = now;
        try {
            boolean requested = this.mWifi.startScan();
            if (requested) {
                Log.d(this.TAG, "WIFI_DIAG event=request accepted=true intervalMs=" + intervalMs + " mode=" + getModeName());
            } else {
                logThrottled(now);
            }
        } catch (SecurityException e) {
            Log.w(this.TAG, "WiFi scan request denied by Android permissions/policy", e);
        }
    }

    private long getScanIntervalMs() {
        return isDebugBypassEnabled() ? DEBUG_BYPASS_SCAN_INTERVAL_MS : THROTTLED_SCAN_INTERVAL_MS;
    }

    private boolean isDebugBypassEnabled() {
        if (!BuildConfig.DEBUG) {
            return false;
        }
        try {
            int developerOptions = Settings.Global.getInt(this.appContext.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
            int wifiScanThrottleEnabled = Settings.Global.getInt(this.appContext.getContentResolver(), "wifi_scan_throttle_enabled", 1);
            return developerOptions == 1 && wifiScanThrottleEnabled == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String getModeName() {
        return isDebugBypassEnabled() ? "debug-1hz" : "normal-throttled";
    }

    private void publishCachedResults() {
        List<ScanResult> results = getScanResultsSafely();
        if (results != null) {
            publishResults(results);
        }
    }

    private List<ScanResult> getScanResultsSafely() {
        if (this.mWifi == null) {
            return null;
        }
        try {
            return this.mWifi.getScanResults();
        } catch (SecurityException e) {
            Log.w(this.TAG, "WiFi scan results denied by Android permissions/policy", e);
            return null;
        }
    }

    private void publishResults(List<ScanResult> results) {
        this.wifiScans.onNext(results);
    }

    private void logThrottled(long now) {
        if (now - this.lastThrottleLogMs < THROTTLE_LOG_INTERVAL_MS) {
            return;
        }
        this.lastThrottleLogMs = now;
        logDiagnostic("WiFi scan request was throttled/rejected; cached results are used; mode=" + getModeName(), true);
    }

    private void logPeriodicDiagnostic() {
        if (!BuildConfig.DEBUG) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        boolean debugBypass = isDebugBypassEnabled();
        if (debugBypass != this.lastDebugBypass) {
            this.lastDebugBypass = debugBypass;
            logDiagnostic("WiFi scan mode changed to " + getModeName() + "; intervalMs=" + getScanIntervalMs(), true);
            return;
        }
        if (now - this.lastDiagnosticLogMs < DIAGNOSTIC_LOG_INTERVAL_MS) {
            return;
        }
        this.lastDiagnosticLogMs = now;
        long freshAgeMs = this.lastFreshResultsMs > 0 ? now - this.lastFreshResultsMs : -1;
        Log.d(this.TAG, "WIFI_DIAG event=status mode=" + getModeName() + " intervalMs=" + getScanIntervalMs() + " freshAgeMs=" + freshAgeMs);
    }

    private void logDiagnostic(String message, boolean inGameLog) {
        String fullMessage = "WIFI_DIAG " + message;
        Log.w(this.TAG, fullMessage);
        if (!BuildConfig.DEBUG || !inGameLog) {
            return;
        }
        try {
            Logger.d(fullMessage);
        } catch (Exception e) {
            Log.d(this.TAG, "Logger is not ready for WiFi diagnostics", e);
        }
    }
}
