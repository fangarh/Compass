package net.afterday.compas.engine.influences.BluetoothInfluences;

import android.support.v4.util.Pair;
import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.engine.influences.InfluenceExtractionStrategy;
import net.afterday.compas.persistency.influences.InfluencesPersistency;
import net.afterday.compas.sensors.Bluetooth.Bluetooth;
import net.afterday.compas.sensors.Bluetooth.BluetoothScanResult;

/* JADX INFO: loaded from: classes.dex */
public class BluetoothInfluenceProviderImpl implements BluetoothInfluenceProvider {
    private static final int EMITTING_INTERVAL = 1000;
    private static final String RUNNING = "R";
    private static final String STOPPED = "S";
    private static final String TAG = "BluetoothInflProvider";
    private static final InfluenceExtractionStrategy<List<BluetoothScanResult>, Double> extractionStrategy = new BluetoothExtractionStrategy();
    private final Bluetooth bluetooth;
    private final Observable<Double> blScans = BehaviorSubject.createDefault(Double.valueOf(-9.99999999999999E10d));
    private final Subject<String> providerState = BehaviorSubject.createDefault(STOPPED);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Observable<Long> providerRunning = this.providerState.switchMap($$Lambda$BluetoothInfluenceProviderImpl$pddAmvQMD0iWhVwLaXWRyPRN8.INSTANCE);

    public BluetoothInfluenceProviderImpl(Bluetooth bluetooth, InfluencesPersistency ip) {
        this.providerState.filter($$Lambda$BluetoothInfluenceProviderImpl$HJGCllSwoLTI5dESv7SN1df7tgw.INSTANCE).switchMap(new $$Lambda$BluetoothInfluenceProviderImpl$Rn60cUvzb2F3e4O8nroqocNCPo(bluetooth)).observeOn(Schedulers.computation()).doOnNext($$Lambda$BluetoothInfluenceProviderImpl$PiykWSlWJ_wNSjJD4bbQHFtXMgo.INSTANCE).subscribe(new $$Lambda$BluetoothInfluenceProviderImpl$MdhRiyDuapi3H8F6c31OGyHtnys(this));
        this.bluetooth = bluetooth;
    }

    static /* synthetic */ ObservableSource lambda$new$0(String s) {
        return s == RUNNING ? Observable.interval(1000L, TimeUnit.MILLISECONDS) : Observable.empty();
    }

    static /* synthetic */ boolean lambda$new$1(String ps) {
        return ps == RUNNING;
    }

    static /* synthetic */ ObservableSource lambda$new$2(Bluetooth bluetooth, String ps) {
        return bluetooth.getSensorResultsStream();
    }

    static /* synthetic */ void lambda$new$3(Double e) {
        Log.e(TAG, "AAAAAAAAAAAAA ---- " + e);
    }

    public /* synthetic */ void lambda$new$4$BluetoothInfluenceProviderImpl(Double i) {
        ((Subject) this.blScans).onNext(i);
    }

    @Override // net.afterday.compas.engine.influences.InfluenceProvider
    public Observable<Double> getInfluenceStream() {
        return this.blScans;
    }

    @Override // net.afterday.compas.engine.influences.InfluenceProvider
    public void start() {
        if (!this.isRunning.get()) {
            this.isRunning.set(true);
            this.bluetooth.start();
            this.providerState.onNext(RUNNING);
        }
    }

    @Override // net.afterday.compas.engine.influences.InfluenceProvider
    public void stop() {
        this.isRunning.set(false);
        this.providerState.onNext(STOPPED);
        this.bluetooth.stop();
    }

    private static class BluetoothResultsScanner {
        private Map<String, Pair<Integer, Long>> buffer;

        BluetoothResultsScanner() {
        }

        InfluencesPack scan(Pair<String, Integer> scanRes) {
            return null;
        }
    }
}
