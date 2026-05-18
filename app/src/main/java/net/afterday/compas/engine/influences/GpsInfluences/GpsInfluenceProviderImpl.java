package net.afterday.compas.engine.influences.GpsInfluences;

import io.reactivex.Observable;
import net.afterday.compas.sensors.Gps.Gps;

/* JADX INFO: loaded from: classes.dex */
public class GpsInfluenceProviderImpl implements GpsInfluenceProvider {
    private Gps gps;

    public GpsInfluenceProviderImpl(Gps gps) {
        this.gps = gps;
    }

    @Override // net.afterday.compas.engine.influences.InfluenceProvider
    public Observable<Integer> getInfluenceStream() {
        return this.gps.getSensorResultsStream();
    }

    @Override // net.afterday.compas.engine.influences.InfluenceProvider
    public void start() {
        this.gps.start();
    }

    @Override // net.afterday.compas.engine.influences.InfluenceProvider
    public void stop() {
        this.gps.stop();
    }
}
