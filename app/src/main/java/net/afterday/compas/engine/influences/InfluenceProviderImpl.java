package net.afterday.compas.engine.influences;

import android.net.wifi.ScanResult;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.List;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.engine.influences.BluetoothInfluences.BluetoothInfluenceProvider;
import net.afterday.compas.engine.influences.BluetoothInfluences.BluetoothInfluenceProviderImpl;
import net.afterday.compas.engine.influences.GpsInfluences.GpsInfluenceProvider;
import net.afterday.compas.engine.influences.GpsInfluences.GpsInfluenceProviderImpl;
import net.afterday.compas.engine.influences.WifiInfluences.WiFiInfluenceProvider;
import net.afterday.compas.engine.influences.WifiInfluences.WifiInfluenceProviderImpl;
import net.afterday.compas.persistency.influences.InfluencesPersistency;
import net.afterday.compas.sensors.Sensor;
import net.afterday.compas.sensors.SensorsProvider;

/* JADX INFO: loaded from: classes.dex */
public class InfluenceProviderImpl implements InfluencesController {
    private static final String TAG = "InfluenceProviderImpl";
    private BluetoothInfluenceProvider bip;
    private Observable<Double> blInfls;
    private GpsInfluenceProvider gip;
    private Observable<Integer> gpsInfls;
    private InfluencesPersistency ip;
    private SensorsProvider sp;
    private Observable<InfluencesPack> wifiInfls;
    private Sensor<List<ScanResult>> wifiSensor;
    private WiFiInfluenceProvider wip;
    private Observable<InfluencesPack> influences = PublishSubject.create();
    private Subject<Boolean> emissionRunning = PublishSubject.create();
    private CompositeDisposable cd = new CompositeDisposable();
    private Subject<Integer> influencesState = BehaviorSubject.createDefault(0);
    private boolean emission = false;

    public InfluenceProviderImpl(SensorsProvider sp, InfluencesPersistency ip, Observable<Long> ticks) {
        this.sp = sp;
        this.ip = ip;
        this.wip = new WifiInfluenceProviderImpl(sp.getWifiSensor(), ip);
        this.bip = new BluetoothInfluenceProviderImpl(sp.getBluetoothSensor(), ip);
        this.gip = new GpsInfluenceProviderImpl(sp.getGpsSensor());
        this.blInfls = this.bip.getInfluenceStream();
        this.wifiInfls = this.wip.getInfluenceStream();
        this.gpsInfls = this.gip.getInfluenceStream();
        ticks.withLatestFrom(this.wifiInfls, this.blInfls, this.gpsInfls, new $$Lambda$InfluenceProviderImpl$8gkGMqrJVqnIp510uisvEwaW9w(this)).subscribe(new $$Lambda$InfluenceProviderImpl$FFGeesoLJFVyqiPAyECX0D0oeFs(this));
    }

    public /* synthetic */ InfluencesPack lambda$new$0$InfluenceProviderImpl(Long t, InfluencesPack w, Double b, Integer g) {
        return combineInfls(w, b, g);
    }

    public /* synthetic */ void lambda$new$1$InfluenceProviderImpl(InfluencesPack inflP) {
        ((Subject) this.influences).onNext(inflP);
    }

    @Override // net.afterday.compas.engine.influences.InfluenceProvider
    public Observable<InfluencesPack> getInfluenceStream() {
        return this.influences;
    }

    @Override // net.afterday.compas.engine.influences.InfluenceProvider
    public void start() {
        this.wip.start();
    }

    @Override // net.afterday.compas.engine.influences.InfluenceProvider
    public void stop() {
        this.wip.stop();
    }

    @Override // net.afterday.compas.engine.influences.InfluencesController
    public void start(int level) {
        this.wip.start();
        this.bip.start();
        if (level >= 5) {
            this.bip.start();
        }
    }

    @Override // net.afterday.compas.engine.influences.InfluencesController
    public void stop(int level) {
        this.wip.stop();
        this.bip.stop();
    }

    @Override // net.afterday.compas.engine.influences.InfluencesController
    public void startEmission() {
        this.emission = true;
        this.gip.start();
    }

    @Override // net.afterday.compas.engine.influences.InfluencesController
    public void stopEmission() {
        this.emission = false;
        this.gip.stop();
    }

    private InfluencesPack combineInfls(InfluencesPack infls, Double blStrength, Integer satelites) {
        if (blStrength != null) {
            infls.addInfluence(6, blStrength.doubleValue());
        }
        if (this.emission) {
            infls.setEmission(true);
            infls.addInfluence(8, satelites.intValue());
        }
        return infls;
    }
}
