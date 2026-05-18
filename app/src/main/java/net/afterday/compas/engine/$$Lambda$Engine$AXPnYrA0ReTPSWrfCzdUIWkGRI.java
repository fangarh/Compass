package net.afterday.compas.engine;

import android.support.v4.util.Pair;
import io.reactivex.functions.Consumer;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$AXPnYrA0ReTPSWrfCzdU-IWkGRI, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$AXPnYrA0ReTPSWrfCzdUIWkGRI implements Consumer {
    private final /* synthetic */ Engine f$0;

    public /* synthetic */ $$Lambda$Engine$AXPnYrA0ReTPSWrfCzdUIWkGRI(Engine engine) {
        this.f$0 = engine;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$initializeGame$6$Engine((Pair) obj);
    }
}
