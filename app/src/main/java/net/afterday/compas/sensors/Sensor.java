package net.afterday.compas.sensors;

import io.reactivex.Observable;

/* JADX INFO: loaded from: classes.dex */
public interface Sensor<T> {
    Observable<T> getSensorResultsStream();

    void start();

    void stop();
}
