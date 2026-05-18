package net.afterday.compas.engine.influences.WifiInfluences;

import android.net.wifi.ScanResult;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.util.List;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.engine.influences.InflPack;
import net.afterday.compas.engine.influences.InfluenceExtractionStrategy;
import net.afterday.compas.engine.threading.Threads;
import net.afterday.compas.persistency.influences.InfluencesPersistency;
import net.afterday.compas.sensors.WiFi.WiFi;

/* JADX INFO: loaded from: classes.dex */
public class WifiInfluenceProviderImpl implements WiFiInfluenceProvider {
    private static final String TAG = "WifiInflProvider";
    private InfluenceExtractionStrategy<List<ScanResult>, InfluencesPack> ies;
    private InfluencesPersistency ip;
    private double lastHealingStrength;
    private Observable<List<ScanResult>> scanResults;
    private WiFi wifi;
    private Observable<InfluencesPack> wifiInfluence = BehaviorSubject.createDefault(new InflPack());
    private boolean wasHealing = false;
    private int noHealing = 0;
    private long lastHealingTime = 0;

    public WifiInfluenceProviderImpl(WiFi wifi, InfluencesPersistency ip) {
        this.wifi = wifi;
        this.ip = ip;
        this.ies = new ByMacExtractionStrategy(ip.getRegisteredWifiModules());
        InfluenceExtractionStrategy<List<ScanResult>, InfluencesPack> influenceExtractionStrategy = this.ies;
        influenceExtractionStrategy.getClass();
        Observable<List<ScanResult>> observableObserveOn = wifi.getSensorResultsStream().observeOn(Threads.computation());
        InfluenceExtractionStrategy<List<ScanResult>, InfluencesPack> influenceExtractionStrategy2 = this.ies;
        influenceExtractionStrategy2.getClass();
        observableObserveOn.map(new $$Lambda$fz7lw2KASQG9uX8iZ6_I47eug(influenceExtractionStrategy2)).map(new $$Lambda$WifiInfluenceProviderImpl$wGzK6zXwHUTRAZ_j9frk3vBz9U(this)).subscribe(new $$Lambda$WifiInfluenceProviderImpl$fLlMdD61IeONjv_xmqFkxJz3xU(this));
    }

    public /* synthetic */ InfluencesPack lambda$new$0$WifiInfluenceProviderImpl(InfluencesPack i) {
        return verifyHealing(i);
    }

    public /* synthetic */ void lambda$new$1$WifiInfluenceProviderImpl(InfluencesPack i) {
        ((Subject) this.wifiInfluence).onNext(i);
    }

    @Override // net.afterday.compas.engine.influences.InfluenceProvider
    public Observable<InfluencesPack> getInfluenceStream() {
        return this.wifiInfluence;
    }

    @Override // net.afterday.compas.engine.influences.InfluenceProvider
    public void start() {
        this.wifi.start();
    }

    @Override // net.afterday.compas.engine.influences.InfluenceProvider
    public void stop() {
        this.wifi.stop();
        ((Subject) this.wifiInfluence).onNext(new InflPack());
    }

    public InfluencesPack verifyHealing(InfluencesPack ip) {
        if (ip.influencedBy(5)) {
            this.lastHealingStrength = ip.getInfluence(5);
            this.lastHealingTime = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - this.lastHealingTime < 5000) {
            ip.addInfluence(5, this.lastHealingStrength);
            this.lastHealingStrength = 0.0d;
            this.lastHealingTime = 0L;
        }
        return ip;
    }
}
