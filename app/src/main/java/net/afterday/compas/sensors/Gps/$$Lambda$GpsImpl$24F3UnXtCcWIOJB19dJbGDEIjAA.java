package net.afterday.compas.sensors.Gps;

import io.reactivex.functions.Consumer;

/* JADX INFO: renamed from: net.afterday.compas.sensors.Gps.-$$Lambda$GpsImpl$24F3UnXtCcWIOJB19dJbGDEIjAA, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$GpsImpl$24F3UnXtCcWIOJB19dJbGDEIjAA implements Consumer {
    private final /* synthetic */ GpsImpl f$0;

    public /* synthetic */ $$Lambda$GpsImpl$24F3UnXtCcWIOJB19dJbGDEIjAA(GpsImpl gpsImpl) {
        this.f$0 = gpsImpl;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$start$0$GpsImpl((Long) obj);
    }
}
