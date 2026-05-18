package net.afterday.compas.engine.influences;

import io.reactivex.Observable;

/* JADX INFO: loaded from: classes.dex */
public interface InfluenceProvider<T> {
    public static final int BLUETOOTH = 2;
    public static final int GPS = 4;
    public static final int WIFI = 1;

    Observable<T> getInfluenceStream();

    void start();

    void stop();
}
