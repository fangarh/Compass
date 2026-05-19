package net.afterday.compas.sensors.WiFi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemClock;
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
import net.afterday.compas.logging.FieldDiagnosticLog;
import net.afterday.compas.logging.Logger;

/* JADX INFO: loaded from: classes.dex */
public class WifiImpl implements WiFi {
    private static final long DEFAULT_SCAN_INTERVAL_MS = 1000;
    private static final long TICK_INTERVAL_SECONDS = 1;
    private static final long THROTTLE_LOG_INTERVAL_MS = 30000;
    private static final long DIAGNOSTIC_LOG_INTERVAL_MS = 30000;
    private static final long CACHED_DETAIL_LOG_INTERVAL_MS = 30000;
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
    private long lastCachedDetailLogMs = 0;
    private int lastCachedResultsCount = -1;
    private boolean lastOneHzMode = true;

    public WifiImpl(Context context) {
        this.appContext = context.getApplicationContext();
        FieldDiagnosticLog.start(this.appContext);
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
        logFreshnessTick();
        logPeriodicDiagnostic();
    }

    @Override // net.afterday.compas.sensors.Sensor
    public void start() {
        Log.d(this.TAG, "WIFI Sensor started " + Thread.currentThread().getName());
        FieldDiagnosticLog.wifi("event=sensor_start thread=" + Thread.currentThread().getName() + " mode=" + getModeName() + " intervalMs=" + getScanIntervalMs());
        registerScanReceiver();
        this.isRunning.set(true);
        this.lastOneHzMode = isOneHzModeEnabled();
        logDiagnostic("WiFi scan mode=" + getModeName() + "; intervalMs=" + getScanIntervalMs() + "; debugBuild=" + BuildConfig.DEBUG, true);
        this.isRunningSubj.onNext(true);
        requestScanIfDue(true);
        publishCachedResults();
    }

    @Override // net.afterday.compas.sensors.Sensor
    public void stop() {
        FieldDiagnosticLog.wifi("event=sensor_stop mode=" + getModeName());
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
        FieldDiagnosticLog.wifi("event=receiver_registered sdk=" + Build.VERSION.SDK_INT);
    }

    private void unregisterScanReceiver() {
        if (!this.scanReceiverRegistered) {
            return;
        }
        try {
            this.appContext.unregisterReceiver(this.scanReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(this.TAG, "WiFi scan receiver was already unregistered", e);
            FieldDiagnosticLog.wifi("event=receiver_unregister_failed error=\"" + e.getClass().getSimpleName() + "\"");
        }
        this.scanReceiverRegistered = false;
        FieldDiagnosticLog.wifi("event=receiver_unregistered");
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
        FieldDiagnosticLog.wifi("event=results source=receiver updated=" + resultsUpdated + " count=" + results.size() + " mode=" + getModeName());
        FieldDiagnosticLog.wifiScanResults("receiver", resultsUpdated, results);
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
                FieldDiagnosticLog.wifi("event=request accepted=true force=" + force + " intervalMs=" + intervalMs + " mode=" + getModeName());
            } else {
                FieldDiagnosticLog.wifi("event=request accepted=false force=" + force + " intervalMs=" + intervalMs + " mode=" + getModeName());
                logThrottled(now);
            }
        } catch (SecurityException e) {
            Log.w(this.TAG, "WiFi scan request denied by Android permissions/policy", e);
            FieldDiagnosticLog.wifi("event=request denied=true error=\"SecurityException\" message=\"" + e.getMessage() + "\"");
        }
    }

    private long getScanIntervalMs() {
        return DEFAULT_SCAN_INTERVAL_MS;
    }

    private boolean isOneHzModeEnabled() {
        return true;
    }

    private String getModeName() {
        return "diagnostic-1s";
    }

    private void publishCachedResults() {
        List<ScanResult> results = getScanResultsSafely();
        if (results != null) {
            this.lastCachedResultsCount = results.size();
            FieldDiagnosticLog.wifi("event=results source=cached count=" + results.size() + " mode=" + getModeName());
            long now = SystemClock.elapsedRealtime();
            if (now - this.lastCachedDetailLogMs >= CACHED_DETAIL_LOG_INTERVAL_MS) {
                this.lastCachedDetailLogMs = now;
                FieldDiagnosticLog.wifiScanResults("cached", false, results);
            }
            publishResults(results);
        }
    }

    private void logFreshnessTick() {
        long now = SystemClock.elapsedRealtime();
        long freshAgeMs = this.lastFreshResultsMs > 0 ? now - this.lastFreshResultsMs : -1;
        FieldDiagnosticLog.wifi("event=tick mode=" + getModeName() + " intervalMs=" + getScanIntervalMs() + " freshAgeMs=" + freshAgeMs + " cachedCount=" + this.lastCachedResultsCount);
    }

    private List<ScanResult> getScanResultsSafely() {
        if (this.mWifi == null) {
            return null;
        }
        try {
            return this.mWifi.getScanResults();
        } catch (SecurityException e) {
            Log.w(this.TAG, "WiFi scan results denied by Android permissions/policy", e);
            FieldDiagnosticLog.wifi("event=results denied=true error=\"SecurityException\" message=\"" + e.getMessage() + "\"");
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
        boolean oneHzMode = isOneHzModeEnabled();
        if (oneHzMode != this.lastOneHzMode) {
            this.lastOneHzMode = oneHzMode;
            logDiagnostic("WiFi scan mode changed to " + getModeName() + "; intervalMs=" + getScanIntervalMs(), true);
            return;
        }
        if (now - this.lastDiagnosticLogMs < DIAGNOSTIC_LOG_INTERVAL_MS) {
            return;
        }
        this.lastDiagnosticLogMs = now;
        long freshAgeMs = this.lastFreshResultsMs > 0 ? now - this.lastFreshResultsMs : -1;
        Log.d(this.TAG, "WIFI_DIAG event=status mode=" + getModeName() + " intervalMs=" + getScanIntervalMs() + " freshAgeMs=" + freshAgeMs);
        FieldDiagnosticLog.wifi("event=status mode=" + getModeName() + " intervalMs=" + getScanIntervalMs() + " freshAgeMs=" + freshAgeMs);
    }

    private void logDiagnostic(String message, boolean inGameLog) {
        String fullMessage = "WIFI_DIAG " + message;
        Log.w(this.TAG, fullMessage);
        FieldDiagnosticLog.wifi(message);
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
