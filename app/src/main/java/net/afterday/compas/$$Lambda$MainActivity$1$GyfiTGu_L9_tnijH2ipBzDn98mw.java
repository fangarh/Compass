package net.afterday.compas;

import io.reactivex.functions.Consumer;
import net.afterday.compas.MainActivity;
import net.afterday.compas.sensors.Battery.BatteryStatus;

/* JADX INFO: renamed from: net.afterday.compas.-$$Lambda$MainActivity$1$GyfiTGu_L9_tnijH2ipBzDn98mw, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$MainActivity$1$GyfiTGu_L9_tnijH2ipBzDn98mw implements Consumer {
    private final /* synthetic */ MainActivity.AnonymousClass1 f$0;

    public /* synthetic */ $$Lambda$MainActivity$1$GyfiTGu_L9_tnijH2ipBzDn98mw(MainActivity.AnonymousClass1 anonymousClass1) {
        this.f$0 = anonymousClass1;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$onServiceConnected$6$MainActivity$1((BatteryStatus) obj);
    }
}
