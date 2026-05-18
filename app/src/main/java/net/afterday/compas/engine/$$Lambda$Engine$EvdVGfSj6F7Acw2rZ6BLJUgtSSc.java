package net.afterday.compas.engine;

import io.reactivex.functions.Consumer;

/* JADX INFO: renamed from: net.afterday.compas.engine.-$$Lambda$Engine$EvdVGfSj6F7Acw2rZ6BLJUgtSSc, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Engine$EvdVGfSj6F7Acw2rZ6BLJUgtSSc implements Consumer {
    private final /* synthetic */ Engine f$0;

    public /* synthetic */ $$Lambda$Engine$EvdVGfSj6F7Acw2rZ6BLJUgtSSc(Engine engine) {
        this.f$0 = engine;
    }

    @Override // io.reactivex.functions.Consumer
    public final void accept(Object obj) {
        this.f$0.lambda$startEmission$19$Engine((Long) obj);
    }
}
